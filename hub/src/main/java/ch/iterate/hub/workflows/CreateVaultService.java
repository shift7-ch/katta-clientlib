/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.pool.SessionPool;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.threading.BackgroundActionState;
import ch.cyberduck.core.vault.VaultRegistryFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.api.StorageResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.CreateS3STSBucketDto;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.core.callback.CreateVaultModel;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEAutomaticAccessGrantDto;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEBackendDto;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.model.StorageProfileDtoWrapperException;
import ch.iterate.hub.protocols.hub.HubCryptoVault;
import ch.iterate.hub.protocols.hub.HubSession;
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
    private final Controller controller;

    public CreateVaultService(final HubSession hubSession, final Controller controller) {
        this.hubSession = hubSession;
        this.controller = controller;
    }

    public void createVault(final CreateVaultModel m) throws ApiException, AccessException, SecurityFailure, BackgroundException {
        try {
            final UsersResourceApi usersResourceApi = new UsersResourceApi(hubSession.getClient());
            final VaultResourceApi vaultResourceApi = new VaultResourceApi(hubSession.getClient());
            final StorageResourceApi storageResourceApi = new StorageResourceApi(hubSession.getClient());
            final StorageProfileResourceApi storageProfilesResourceApi = new StorageProfileResourceApi(hubSession.getClient());
            // Get only non-archived profiles to create new vault
            final List<StorageProfileDto> storageProfiles = storageProfilesResourceApi.apiStorageprofileGet(false);
            final StorageProfileDto storageProfile = storageProfiles.stream().filter(c -> {
                try {
                    return StorageProfileDtoWrapper.coerce(c).getId().toString().equals(m.storageProfileId());
                }
                catch(StorageProfileDtoWrapperException e) {
                    log.warn(e);
                    return false;
                }
            }).findFirst().orElse(null);
            final StorageProfileDtoWrapper backend = StorageProfileDtoWrapper.coerce(storageProfile);

            // prepare vault creation
            final UserKeys userKeys = new FirstLoginDeviceSetupService(hubSession).getUserKeysWithDeviceKeys(
                    hubSession.getHost(), FirstLoginDeviceSetupCallbackFactory.get());

            final UvfMetadataPayload.UniversalVaultFormatJWKS jwks;
            jwks = UvfMetadataPayload.createKeys();
            final UvfMetadataPayload metadataJWE;
            try {
                metadataJWE = UvfMetadataPayload
                        .create()
                        .withStorage(new VaultMetadataJWEBackendDto()
                                .provider(backend.getId().toString())
                                .defaultPath(backend.getType().equals(StorageProfileS3STSDto.class) ? backend.getBucketPrefix() + m.uuid() : m.bucketName())
                                .nickname(m.vaultName())
                                .username(m.accessKeyId())
                                .password(m.secretKey()))
                        .withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                                .enabled(m.automaticAccessGrant())
                                .maxWotDepth(m.maxWotLevel())
                        );
            }
            catch(StorageProfileDtoWrapperException e) {
                throw new SecurityFailure(e);
            }
            if(log.isDebugEnabled()) {
                log.debug(String.format("Created metadata JWE %s", metadataJWE));
            }
            final String uvfMetadataFile;
            uvfMetadataFile = metadataJWE.encrypt(
                    String.format("%s/api", new HostUrlProvider(false, true).get(hubSession.getHost())),
                    m.uuid(),
                    jwks.toJWKSet()
            );

            final VaultDto vaultDto = new VaultDto()
                    .id(m.uuid())
                    .name(m.vaultName())
                    .description(m.vaultDescription())
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(uvfMetadataFile)
                    .uvfKeySet(jwks.serializePublicRecoverykey());
            final CreateS3STSBucketDto storageDto;
            try {
                storageDto = new CreateS3STSBucketDto()
                        .vaultId(vaultDto.getId().toString())
                        .storageConfigId(backend.getId())
                        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 do we need to store here as well? Only in VaultDto?
                        .vaultUvf(uvfMetadataFile)
                        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 do we need to store here?
                        .rootDirHash(metadataJWE.computeRootDirIdHash(metadataJWE.computeRootDirId()))
                        .region(m.region());
            }
            catch(StorageProfileDtoWrapperException e) {
                throw new SecurityFailure(e);
            }
            if(log.isDebugEnabled()) {
                log.debug(String.format("Created storage dto %s", storageDto));
            }

            final boolean isPermanent = backend.getType().equals(StorageProfileS3Dto.class);
            if(isPermanent) {
                // permanent: template upload into existing bucket

                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 review @dko
                // N.B. we don't have the vault in the hub yet, so we can't use neither profile sync nor S3AutoloadVaultProtocol!
                final Host bootstrappingBookmark = new Host(new S3Protocol() {
                    @Override
                    public Scheme getScheme() {
                        try {
                            return backend.getScheme() != null ? Scheme.valueOf(backend.getScheme()) : Scheme.https;
                        }
                        catch(StorageProfileDtoWrapperException e) {
                            log.error(e);
                            return Scheme.https;
                        }
                    }

                    @Override
                    public String getPrefix() {
                        return String.format("%s.%s", S3Protocol.class.getPackage().getName(), StringUtils.capitalize(this.getType().name()));
                    }
                });
                if(backend.getHostname() != null) {
                    bootstrappingBookmark.setHostname(backend.getHostname());
                }
                if(backend.getPort() != null) {
                    bootstrappingBookmark.setPort(backend.getPort());
                }
                if((backend.getWithPathStyleAccessEnabled() != null) && backend.getWithPathStyleAccessEnabled()) {
                    bootstrappingBookmark.setProperty("s3.bucket.virtualhost.disable", "true");
                }
                bootstrappingBookmark.setDefaultPath(m.bucketName());
                bootstrappingBookmark.setCredentials(new Credentials(m.accessKeyId(), m.secretKey()));

                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 review @dko
                final SessionPool pool = SessionPoolFactory.create(
                        new LoginConnectionService(LoginCallbackFactory.get(controller), HostKeyCallbackFactory.get(controller, bootstrappingBookmark.getProtocol()), PasswordStoreFactory.get(), controller),
                        controller, bootstrappingBookmark,
                        new KeychainX509TrustManager(CertificateTrustCallbackFactory.get(controller), new DefaultTrustManagerHostnameCallback(bootstrappingBookmark), CertificateStoreFactory.get()),
                        new KeychainX509KeyManager(CertificateIdentityCallbackFactory.get(controller), bootstrappingBookmark, CertificateStoreFactory.get()),
                        VaultRegistryFactory.get(PasswordStoreFactory.get(), PasswordCallbackFactory.get(controller))
                );
                // upload vault template
                new HubCryptoVault(new Path(m.bucketName(), EnumSet.of(AbstractPath.Type.directory, AbstractPath.Type.vault)))
                        .create(pool.borrow(BackgroundActionState.running), m.region(), null, 42, storageDto.getVaultUvf(), storageDto.getRootDirHash());
            }
            else {
                // non-permanent: pass STS tokens (restricted by inline policy) to hub backend and have bucket created from there
                final TemporaryAccessTokens stsTokens = getSTSTokensFromAccessTokenWithCreateBucketInlinePoliy(
                        hubSession.refresh().getAccessToken(),
                        backend.getStsRoleArnClient(),
                        vaultDto.getId().toString(),
                        backend.getStsEndpoint(),
                        String.format("%s%s", backend.getBucketPrefix(), vaultDto.getId()),
                        backend.getBucketAcceleration()
                );
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Create STS bucket %s for vault %s", storageDto, vaultDto));
                }
                storageResourceApi.apiStorageVaultIdPut(vaultDto.getId(),
                        storageDto.awsAccessKey(stsTokens.getAccessKeyId())
                                .awsSecretKey(stsTokens.getSecretAccessKey())
                                .sessionToken(stsTokens.getSessionToken()));
            }
            // create vault in hub
            if(log.isDebugEnabled()) {
                log.debug(String.format("Create vault %s", vaultDto));
            }
            vaultResourceApi.apiVaultsVaultIdPut(
                    vaultDto.getId(),
                    vaultDto
            );

            // upload JWE
            if(log.isDebugEnabled()) {
                log.debug(String.format("Upload JWE %s for vault %s", uvfMetadataFile, vaultDto));
            }
            final UserDto userDto = usersResourceApi.apiUsersMeGet(false);

            vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultDto.getId(), Collections.singletonMap(userDto.getId(), jwks.toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic())));
        }
        catch(JOSEException e) {
            throw new SecurityFailure(e);
        }
        catch(StorageProfileDtoWrapperException e) {
            throw new RuntimeException(e);
        }
        catch(JsonProcessingException e) {
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
}
