/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.Path;
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

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.CreateS3STSBucketDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEAutomaticAccessGrantDto;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEBackendDto;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.protocols.hub.HubCryptoVault;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.HubStorageVaultSyncSchedulerService;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
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

    public CreateVaultService(final HubSession hubSession) {
        this.hubSession = hubSession;
    }

    public void createVault(final StorageProfileDtoWrapper storageProfileWrapper, final UUID vaultUuid, final CreateVaultModel vaultModel) throws ApiException, AccessException, SecurityFailure, BackgroundException {
        try {
            // prepare vault creation
            final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getUserKeys(
                    hubSession.getHost(), FirstLoginDeviceSetupCallbackFactory.get());

            final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
            final UvfMetadataPayload metadataJWE = UvfMetadataPayload.create()
                    .withStorage(new VaultMetadataJWEBackendDto()
                            .provider(storageProfileWrapper.getId().toString())
                            .defaultPath(storageProfileWrapper.getStsEndpoint() != null ? storageProfileWrapper.getBucketPrefix() + vaultUuid : vaultModel.bucketName())
                            .nickname(vaultModel.vaultName())
                            .username(vaultModel.accessKeyId())
                            .password(vaultModel.secretKey()))
                    .withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                            .enabled(vaultModel.automaticAccessGrant())
                            .maxWotDepth(vaultModel.maxWotLevel())
                    );
            if(log.isDebugEnabled()) {
                log.debug(String.format("Created metadata JWE %s", metadataJWE));
            }
            final String uvfMetadataFile = metadataJWE.encrypt(
                    String.format("%s/api", new HostUrlProvider(false, true).get(hubSession.getHost())),
                    vaultUuid,
                    jwks.toJWKSet()
            );
            final VaultDto vaultDto = new VaultDto()
                    .id(vaultUuid)
                    .name(metadataJWE.storage().getNickname())
                    .description(vaultModel.vaultDescription())
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(uvfMetadataFile)
                    .uvfKeySet(jwks.serializePublicRecoverykey());
            final CreateS3STSBucketDto storageDto = new CreateS3STSBucketDto()
                    .vaultId(vaultModel.vaultId().toString())
                    .storageConfigId(storageProfileWrapper.getId())
                    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 do we need to store here as well? Only in VaultDto?
                    .vaultUvf(uvfMetadataFile)
                    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 do we need to store here?
                    .rootDirHash(metadataJWE.computeRootDirIdHash(metadataJWE.computeRootDirId()))
                    .region(metadataJWE.storage().getRegion());
            if(log.isDebugEnabled()) {
                log.debug(String.format("Created storage dto %s", storageDto));
            }
            final Host bookmark = HubStorageVaultSyncSchedulerService.toBookmark(hubSession.getHost(), vaultDto.getId(), metadataJWE.storage());
            if(storageProfileWrapper.getStsEndpoint() == null) {
                // permanent: template upload into existing bucket
                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 review @dko
                final S3Session session = new S3Session(bookmark);
                session.open(new DisabledProxyFinder(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
                session.login(new DisabledLoginCallback(), new DisabledCancelCallback());
                // upload vault template
                new HubCryptoVault(new Path(metadataJWE.storage().getDefaultPath(), EnumSet.of(AbstractPath.Type.directory, AbstractPath.Type.vault)))
                        .create(session, metadataJWE.storage().getRegion(), null, 42, storageDto.getVaultUvf(), storageDto.getRootDirHash());
                session.close();
            }
            else {
                // non-permanent: pass STS tokens (restricted by inline policy) to hub backend and have bucket created from there
                final TemporaryAccessTokens stsTokens = getSTSTokensFromAccessTokenWithCreateBucketInlinePoliy(
                        bookmark.getCredentials().getOauth().getAccessToken(),
                        storageProfileWrapper.getStsRoleArnClient(),
                        vaultDto.getId().toString(),
                        storageProfileWrapper.getStsEndpoint(),
                        String.format("%s%s", storageProfileWrapper.getBucketPrefix(), vaultDto.getId()),
                        storageProfileWrapper.getBucketAcceleration()
                );
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Create STS bucket %s for vault %s", storageDto, vaultDto));
                }
                new StorageResourceApi(hubSession.getClient()).apiStorageVaultIdPut(vaultDto.getId(),
                        storageDto.awsAccessKey(stsTokens.getAccessKeyId())
                                .awsSecretKey(stsTokens.getSecretAccessKey())
                                .sessionToken(stsTokens.getSessionToken()));
            }
            // create vault in hub
            if(log.isDebugEnabled()) {
                log.debug(String.format("Create vault %s", vaultDto));
            }
            new VaultResourceApi(hubSession.getClient()).apiVaultsVaultIdPut(vaultDto.getId(), vaultDto);

            // upload JWE
            if(log.isDebugEnabled()) {
                log.debug(String.format("Upload JWE %s for vault %s", uvfMetadataFile, vaultDto));
            }
            final UserDto userDto = new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(false);
            new VaultResourceApi(hubSession.getClient()).apiVaultsVaultIdAccessTokensPost(vaultDto.getId(), Collections.singletonMap(userDto.getId(), jwks.toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic())));
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
        catch(IOException e) {
            throw new AccessException(e);
        }
    }

    private static TemporaryAccessTokens getSTSTokensFromAccessTokenWithCreateBucketInlinePoliy(final String token, final String roleArn, final String roleSessionName, final String stsEndpoint, final String bucketName, final Boolean bucketAcceleration) throws IOException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Get STS tokens from %s to pass to backend %s with role %s and session name %s", token, stsEndpoint, roleArn, roleSessionName));
        }

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
        if(stsEndpoint != null) {
            serviceBuild.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(stsEndpoint, null));
        }
        final AWSSecurityTokenService service = serviceBuild
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();


        if(log.isDebugEnabled()) {
            log.debug(String.format("Use request %s", request));
        }
        final AssumeRoleWithWebIdentityResult result = service.assumeRoleWithWebIdentity(request);
        if(log.isDebugEnabled()) {
            log.debug(String.format("Received assume role identity result %s", result));
        }
        return new TemporaryAccessTokens(result.getCredentials().getAccessKeyId(),
                result.getCredentials().getSecretAccessKey(),
                result.getCredentials().getSessionToken(),
                result.getCredentials().getExpiration().getTime());
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
