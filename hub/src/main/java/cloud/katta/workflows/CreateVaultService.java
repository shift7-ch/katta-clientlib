/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.TemporaryAccessTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.proxy.DisabledProxyFinder;
import ch.cyberduck.core.s3.S3Session;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.ConfigResourceApi;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.StorageResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.CreateS3STSBucketDto;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataJWEAutomaticAccessGrantDto;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubUVFVault;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

/**
 * Create a vault in hub from CreateVaultModel.
 */
public class CreateVaultService {
    private static final Logger log = LogManager.getLogger(CreateVaultService.class);

    private final HubSession hubSession;
    private final ConfigResourceApi configResource;
    private final VaultResourceApi vaultResource;
    private final StorageProfileResourceApi storageProfileResource;
    private final StorageResourceApi storageResource;
    private final UsersResourceApi users;
    private final TemplateUploadService templateUploadService;
    private final STSInlinePolicyService stsInlinePolicyService;

    public CreateVaultService(final HubSession hubSession) {
        this(hubSession, new ConfigResourceApi(hubSession.getClient()), new VaultResourceApi(hubSession.getClient()), new StorageProfileResourceApi(hubSession.getClient()), new UsersResourceApi(hubSession.getClient()), new StorageResourceApi(hubSession.getClient()), new TemplateUploadService(), new STSInlinePolicyService());
    }

    CreateVaultService(final HubSession hubSession, final ConfigResourceApi configResource, final VaultResourceApi vaultResource, final StorageProfileResourceApi storageProfileResource, final UsersResourceApi users, final StorageResourceApi storageResource, final TemplateUploadService templateUploadService, final STSInlinePolicyService stsInlinePolicyService) {
        this.hubSession = hubSession;
        this.configResource = configResource;
        this.vaultResource = vaultResource;
        this.storageProfileResource = storageProfileResource;
        this.storageResource = storageResource;
        this.users = users;
        this.templateUploadService = templateUploadService;
        this.stsInlinePolicyService = stsInlinePolicyService;
    }

    public void createVault(final UserKeys userKeys, final StorageProfileDtoWrapper storageProfileWrapper, final CreateVaultModel vaultModel) throws ApiException, AccessException, SecurityFailure, BackgroundException {
        try {
            final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
            final UvfMetadataPayload metadataPayload = UvfMetadataPayload.create()
                    .withStorage(new VaultMetadataJWEBackendDto()
                            .provider(storageProfileWrapper.getId().toString())
                            .defaultPath(storageProfileWrapper.getProtocol() == Protocol.S3_STS ? storageProfileWrapper.getBucketPrefix() + vaultModel.vaultId() : vaultModel.bucketName())
                            .nickname(vaultModel.vaultName())
                            .username(vaultModel.accessKeyId())
                            .password(vaultModel.secretKey()))
                    .withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                            .enabled(vaultModel.automaticAccessGrant())
                            .maxWotDepth(vaultModel.maxWotLevel())
                    );
            log.debug("Created metadata JWE {}", metadataPayload);
            final String uvfMetadataFile = metadataPayload.encrypt(
                    String.format("%s/api", new HostUrlProvider(false, true).get(hubSession.getHost())),
                    vaultModel.vaultId(),
                    jwks.toJWKSet()
            );
            final VaultDto vaultDto = new VaultDto()
                    .id(vaultModel.vaultId())
                    .name(metadataPayload.storage().getNickname())
                    .description(vaultModel.vaultDescription())
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(uvfMetadataFile)
                    .uvfKeySet(jwks.serializePublicRecoverykey());

            final String hashedRootDirId = metadataPayload.computeRootDirIdHash();
            final byte[] rootDirUvf = metadataPayload.computeRootDirUvf();
            final CreateS3STSBucketDto storageDto = new CreateS3STSBucketDto()
                    .vaultId(vaultModel.vaultId().toString())
                    .storageConfigId(storageProfileWrapper.getId())
                    .vaultUvf(uvfMetadataFile)
                    .rootDirHash(hashedRootDirId)
                    .region(metadataPayload.storage().getRegion());
            log.debug("Created storage dto {}", storageDto);

            final HostPasswordStore keychain = PasswordStoreFactory.get();

            final OAuthTokens tokens = keychain.findOAuthTokens(hubSession.getHost());
            final Host bookmark = new VaultServiceImpl(vaultResource, storageProfileResource).getStorageBackend(ProtocolFactory.get(),
                    configResource.apiConfigGet(), vaultDto.getId(), metadataPayload.storage(), tokens);
            if(storageProfileWrapper.getProtocol() == Protocol.S3) {
                // permanent: template upload into existing bucket from client (not backend)
                templateUploadService.uploadTemplate(bookmark, metadataPayload, storageDto, hashedRootDirId, rootDirUvf);
            }
            else {
                // non-permanent: pass STS tokens (restricted by inline policy) to hub backend and have bucket created from there
                final TemporaryAccessTokens stsTokens = stsInlinePolicyService.getSTSTokensFromAccessTokenWithCreateBucketInlinePolicy(
                        tokens.getAccessToken(),
                        storageProfileWrapper.getStsRoleArnClient(),
                        vaultDto.getId().toString(),
                        storageProfileWrapper.getStsEndpoint(),
                        String.format("%s%s", storageProfileWrapper.getBucketPrefix(), vaultDto.getId()),
                        vaultModel.region(),
                        storageProfileWrapper.getBucketAcceleration()
                );
                log.debug("Create STS bucket {} for vault {}", storageDto, vaultDto);
                storageResource.apiStorageVaultIdPut(vaultDto.getId(),
                        storageDto.awsAccessKey(stsTokens.getAccessKeyId())
                                .awsSecretKey(stsTokens.getSecretAccessKey())
                                .sessionToken(stsTokens.getSessionToken()));
            }
            // create vault in hub
            log.debug("Create vault {}", vaultDto);
            vaultResource.apiVaultsVaultIdPut(vaultDto.getId(), vaultDto);

            // upload JWE
            log.debug("Upload JWE {} for vault {}", uvfMetadataFile, vaultDto);
            final UserDto userDto = users.apiUsersMeGet(false, false);
            vaultResource.apiVaultsVaultIdAccessTokensPost(vaultDto.getId(), Collections.singletonMap(userDto.getId(), jwks.toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic())));
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
        catch(IOException e) {
            throw new AccessException(e);
        }
    }

    static class TemplateUploadService {
        static TemplateUploadService disabled = new TemplateUploadService() {
            @Override
            void uploadTemplate(final Host bookmark, final UvfMetadataPayload metadataPayload, final CreateS3STSBucketDto storageDto, final String hashedRootDirId, final byte[] rootDirUvf) {
                // do nothing
            }
        };

        void uploadTemplate(final Host bookmark, final UvfMetadataPayload metadataPayload, final CreateS3STSBucketDto storageDto, final String hashedRootDirId, final byte[] rootDirUvf) throws BackgroundException {
            final S3Session session = new S3Session(bookmark);
            session.open(new DisabledProxyFinder(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
            session.login(new DisabledLoginCallback(), new DisabledCancelCallback());

            // upload vault template
            new HubUVFVault(session, new Path(metadataPayload.storage().getDefaultPath(), EnumSet.of(Path.Type.directory, Path.Type.vault)))
                    .create(session, metadataPayload.storage().getRegion(), storageDto.getVaultUvf(), hashedRootDirId, rootDirUvf);
            session.close();
        }
    }

    static class STSInlinePolicyService {
        static STSInlinePolicyService disabled = new STSInlinePolicyService() {
            @Override
            TemporaryAccessTokens getSTSTokensFromAccessTokenWithCreateBucketInlinePolicy(final String token, final String roleArn, final String roleSessionName, final String stsEndpoint, final String bucketName, final String region, final Boolean bucketAcceleration) {
                return new TemporaryAccessTokens(null);
            }
        };

        TemporaryAccessTokens getSTSTokensFromAccessTokenWithCreateBucketInlinePolicy(final String token, final String roleArn, final String roleSessionName, final String stsEndpoint, final String bucketName, final String region, final Boolean bucketAcceleration) throws IOException {
            log.debug("Get STS tokens from {} to pass to backend {} with role {} and session name {}", token, stsEndpoint, roleArn, roleSessionName);

            final AssumeRoleWithWebIdentityRequest request = new AssumeRoleWithWebIdentityRequest();

            request.setWebIdentityToken(token);

            String inlinePolicy = IOUtils.toString(CreateVaultService.class.getResourceAsStream("/sts_create_bucket_inline_policy_template.json"), Charset.defaultCharset()).replace("{}", bucketName);
            if((bucketAcceleration != null) && bucketAcceleration) {
                inlinePolicy = inlinePolicy.replace("s3:PutEncryptionConfiguration\"", "s3:PutEncryptionConfiguration\",         \"s3:GetAccelerateConfiguration\",\n        \"s3:PutAccelerateConfiguration\"");
            }

            request.setPolicy(inlinePolicy);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(roleSessionName);

            AWSSecurityTokenServiceClientBuilder serviceBuild = AWSSecurityTokenServiceClientBuilder
                    .standard();
            // Exactly only one of Region or EndpointConfiguration may be set.
            if(stsEndpoint != null) {
                serviceBuild.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(stsEndpoint, null));
            }
            else {
                serviceBuild.withRegion(region);
            }
            final AWSSecurityTokenService service = serviceBuild
                    .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                    .build();

            log.debug("Use request {}", request);
            final AssumeRoleWithWebIdentityResult result = service.assumeRoleWithWebIdentity(request);
            log.debug("Received assume role identity result {}", result);
            return new TemporaryAccessTokens(result.getCredentials().getAccessKeyId(),
                    result.getCredentials().getSecretAccessKey(),
                    result.getCredentials().getSessionToken(),
                    result.getCredentials().getExpiration().getTime());
        }
    }

    public static class CreateVaultModel {

        private final UUID vaultId;
        private final String vaultName;
        private final String vaultDescription;
        private final String storageProfileId;
        private final String accessKeyId;
        private final String secretKey;
        private final String bucketName;
        private final String region;
        private final boolean automaticAccessGrant;
        private final int maxWotLevel;


        public CreateVaultModel(final UUID vaultId, final String vaultName, final String vaultDescription, final String storageProfileId,
                                final String accessKeyId, final String secretKey,
                                final String bucketName, final String region,
                                final boolean automaticAccessGrant, final int maxWotLevel) {
            this.vaultId = vaultId;
            this.vaultName = vaultName;
            this.vaultDescription = vaultDescription;
            this.storageProfileId = storageProfileId;
            this.accessKeyId = accessKeyId;
            this.secretKey = secretKey;
            this.bucketName = bucketName;
            this.region = region;
            this.automaticAccessGrant = automaticAccessGrant;
            this.maxWotLevel = maxWotLevel;
        }

        public UUID vaultId() {
            return vaultId;
        }

        public String vaultName() {
            return vaultName;
        }

        public String vaultDescription() {
            return vaultDescription;
        }

        public String storageProfileId() {
            return storageProfileId;
        }

        public String accessKeyId() {
            return accessKeyId;
        }

        public String secretKey() {
            return secretKey;
        }

        public String bucketName() {
            return bucketName;
        }

        public String region() {
            return region;
        }

        public boolean automaticAccessGrant() {
            return automaticAccessGrant;
        }

        public int maxWotLevel() {
            return maxWotLevel;
        }
    }
}
