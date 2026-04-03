/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DefaultPathAttributes;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.UUIDRandomStringService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.s3.S3Protocol;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSAssumeRoleWithWebIdentityCredentialsStrategy;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultException;
import ch.cyberduck.core.vault.VaultProvider;
import ch.cyberduck.core.vault.VaultUnlockCancelException;
import ch.cyberduck.core.vault.VaultVersion;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataStorageDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.protocols.s3.STSChainedAssumeRoleRequestInterceptor;
import cloud.katta.workflows.VaultServiceImpl;
import cloud.katta.workflows.exceptions.SecurityFailure;

public class HubUVFVaultProvider implements VaultProvider {
    private static final Logger log = LogManager.getLogger(HubUVFVaultProvider.class);

    private final LoginCallback prompt;

    public HubUVFVaultProvider(final LoginCallback prompt) {
        this.prompt = prompt;
    }

    @Override
    public VaultVersion matches(final Path file) {
        return new VaultVersion(VaultVersion.Type.UVF);
    }

    @Override
    public VaultVersion find(final Path directory, final Find find, final ListProgressListener listener) {
        return new VaultVersion(VaultVersion.Type.UVF);
    }

    @Override
    public Vault create(final Session<?> session, final String region, final Path name, final VaultVersion metadata, final VaultCredentials passphrase) throws BackgroundException {
        try {
            final HubStorageLocationService.StorageLocation location = HubStorageLocationService.StorageLocation.fromIdentifier(region);
            // Determine actual bucket name from storage location
            final StorageProfileDtoWrapper storageProfile = StorageProfileDtoWrapper.coerce(new StorageProfileResourceApi(HubSession.coerce(session).getClient())
                    .apiStorageprofileProfileIdGet(UUID.fromString(location.getProfile())));
            final UUID vaultId = UUID.fromString(new UUIDRandomStringService().random());
            final S3Session storage;
            final Path bucket = new Path(storageProfile.getBucketPrefix() + vaultId, EnumSet.of(Path.Type.volume, Path.Type.directory),
                    new DefaultPathAttributes()
                            .setRegion(region)
                            .setDisplayname(name.getName()));
            final UVFMetadataPayload payload;
            switch(storageProfile.getProtocol()) {
                case S3_STATIC: {
                    final Credentials credentials = prompt.prompt(session.getHost(), StringUtils.EMPTY,
                            LocaleFactory.localizedString("Provide additional login credentials", "Credentials"),
                            StringUtils.EMPTY, new LoginOptions(new S3Protocol())
                                    .user(true)
                                    .password(true)
                                    .save(false));
                    log.debug("Use static S3 credentials {}", credentials);
                    payload = location.toPayload(bucket, credentials);
                    storage = new S3Session(new Host(new HubStorageProfile(
                            new S3Protocol(), HubSession.coerce(session).getConfig(), storageProfile), credentials).setRegion(location.getRegion()),
                            session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class));
                    break;
                }
                case S3_STS: {
                    // OAuth Tokens shared with request interceptor
                    final Host host = new Host(new HubStorageProfile(
                            new S3Protocol(), HubSession.coerce(session).getConfig(), storageProfile), session.getHost().getCredentials()) {
                        @Override
                        public String getProperty(final String key) {
                            if(Profile.STS_ROLE_ARN_PROPERTY_KEY.equals(key)) {
                                final String arn = storageProfile.getStsRoleCreateBucketClient();
                                log.debug("Use STS role ARN {} for vault {}", arn, vaultId);
                                return arn;
                            }
                            return super.getProperty(key);
                        }
                    }.setRegion(location.getRegion());
                    payload = location.toPayload(bucket);
                    storage = new S3Session(host, session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class)) {
                        @Override
                        protected S3CredentialsStrategy configureCredentialsStrategy(final HttpClientBuilder configuration, final LoginCallback prompt) {
                            final OAuth2RequestInterceptor interceptor = session.getFeature(OAuth2RequestInterceptor.class);
                            log.debug("Configure with shared OAuth interceptor {}", interceptor);
                            configuration.addInterceptorLast(interceptor);
                            return new STSAssumeRoleWithWebIdentityCredentialsStrategy(interceptor,
                                    host, session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class), prompt);
                        }
                    };
                    break;
                }
                default:
                    log.error("Unsupported storage configuration {} for vault {}", storageProfile.getProtocol(), vaultId);
                    throw new VaultException(storageProfile.getProtocol().toString());
            }
            log.debug("Configured {} for vault {}", storage, vaultId);
            final HubUVFVault vault = new HubUVFVault(storage, bucket, prompt);
            final HubVaultKeys keys = HubVaultKeys.create();
            final HubVaultMetadataUVFProvider vaultMetadataProvider = new HubVaultMetadataUVFProvider(
                    payload.toJSON(HubSession.coerce(session).getClient().getBasePath(), vaultId), keys);
            log.debug("Create vault with ID {}", vaultId);
            final VaultDto vaultDto = new VaultDto()
                    .id(vaultId)
                    .name(name.getName())
                    .description(null)
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(new String(vaultMetadataProvider.encrypt(), StandardCharsets.US_ASCII))
                    .uvfKeySet(keys.serialize().toPublicJWKSet().toString());
            // Create vault in Hub
            final VaultResourceApi vaultResourceApi = new VaultResourceApi(HubSession.coerce(session).getClient());
            log.debug("Create vault {}", vaultDto);
            vaultResourceApi.apiVaultsVaultIdPut(vaultId, vaultDto,
                    storage.getHost().getProtocol().isRoleConfigurable() && !S3Session.isAwsHostname(storage.getHost().getHostname()),
                    storage.getHost().getProtocol().isRoleConfigurable() && S3Session.isAwsHostname(storage.getHost().getHostname()));
            // Upload JWE
            log.debug("Grant access to vault {}", vaultDto);
            final UserDto userDto = HubSession.coerce(session).getMe();
            final DeviceSetupCallback setup = prompt.getFeature(DeviceSetupCallback.class);
            final UserKeys userKeys = HubSession.coerce(session).pair(setup);
            // Share vault with myself including admin access with recovery key
            vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultId, Collections.singletonMap(userDto.getId(),
                    new UVFAccessTokenPayload(keys.memberKey(), keys.recoveryKey()).encryptForUser(userKeys.ecdhKeyPair().getPublic())));
            // Upload metadata to bucket
            vault.create(session, location.getRegion(), vaultMetadataProvider);
            return vault;
        }
        catch(SecurityFailure e) {
            throw new VaultException(e.getMessage(), e);
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }

    }

    @Override
    public Vault load(final Session<?> session, final Path id, final VaultVersion metadata, final VaultCredentials passphrase) throws BackgroundException {
        try {
            final UUID vaultId = UUID.fromString(id.getName());
            final String vaultMetadataFile = new VaultResourceApi(HubSession.coerce(session).getClient()).apiVaultsVaultIdUvfVaultUvfGet(vaultId);
            // Find storage configuration in vault metadata
            final DeviceSetupCallback setup = prompt.getFeature(DeviceSetupCallback.class);
            final VaultServiceImpl vaultService = new VaultServiceImpl(HubSession.coerce(session));
            final UVFAccessTokenPayload accessToken = vaultService.getVaultAccessToken(vaultId, HubSession.coerce(session).pair(setup));
            log.debug("Retrieved vault access token for vault {}", vaultId);
            final UVFMetadataPayload vaultMetadata = vaultService.decryptVaultMetadata(accessToken, vaultMetadataFile);
            log.debug("Decrypted vault metadata {} for vault {}", vaultMetadataFile, vaultId);
            final VaultMetadataStorageDto vaultStorageMetadata = vaultMetadata.storage();
            final HubStorageLocationService.StorageLocation location = HubStorageLocationService.StorageLocation.fromMetadata(vaultStorageMetadata);
            log.debug("Determined storage location {} for vault {}", location, vaultId);
            final StorageProfileDtoWrapper storageProfile = StorageProfileDtoWrapper.coerce(new StorageProfileResourceApi(HubSession.coerce(session).getClient())
                    .apiStorageprofileProfileIdGet(UUID.fromString(location.getProfile())));
            log.debug("Retrieved storage profile {} for vault {}", storageProfile, vaultId);
            final S3Session storage;
            switch(storageProfile.getProtocol()) {
                case S3_STATIC: {
                    final Credentials credentials = new Credentials(vaultStorageMetadata.getUsername(), vaultStorageMetadata.getPassword());
                    storage = new S3Session(new Host(new HubStorageProfile(
                            new S3Protocol(), HubSession.coerce(session).getConfig(), storageProfile), credentials).setRegion(location.getRegion()),
                            session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class));
                    log.debug("Use static S3 credentials {}", credentials);
                    break;
                }
                case S3_STS:
                    // OAuth Tokens shared with request interceptor
                    final Host host = new Host(new HubStorageProfile(
                            new S3Protocol(), HubSession.coerce(session).getConfig(), storageProfile), session.getHost().getCredentials()) {
                        @Override
                        public String getProperty(final String key) {
                            if(Profile.STS_ROLE_ARN_PROPERTY_KEY.equals(key)) {
                                final String arn = storageProfile.getStsRoleAccessBucketAssumeRoleWithWebIdentity();
                                log.debug("Use STS role ARN {} for vault {}", arn, vaultId);
                                return arn;
                            }
                            return super.getProperty(key);
                        }
                    }.setRegion(location.getRegion());
                    storage = new S3Session(host, session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class)) {
                        @Override
                        protected S3CredentialsStrategy configureCredentialsStrategy(final HttpClientBuilder configuration, final LoginCallback prompt) {
                            final OAuth2RequestInterceptor interceptor = session.getFeature(OAuth2RequestInterceptor.class);
                            log.debug("Configure with shared OAuth interceptor {}", interceptor);
                            configuration.addInterceptorLast(interceptor);
                            return new STSChainedAssumeRoleRequestInterceptor(HubSession.coerce(session), interceptor, vaultId,
                                    storageProfile.getStsRoleAccessBucketAssumeRoleTaggedSession(), storageProfile.getStsSessionTag(),
                                    host, session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class));
                        }
                    };
                    break;
                default:
                    log.error("Unsupported storage configuration {} for vault {}", storageProfile.getProtocol(), vaultId);
                    throw new VaultException(storageProfile.getProtocol().toString());
            }
            log.debug("Configured {} for vault {}", storage, vaultId);
            final Path bucket = new Path(vaultStorageMetadata.getDefaultPath() /*Bucket Name*/, EnumSet.of(Path.Type.directory, Path.Type.volume),
                    new DefaultPathAttributes()
                            .setRegion(HubStorageLocationService.StorageLocation.fromMetadata(vaultStorageMetadata).getIdentifier())
                            .setDisplayname(vaultStorageMetadata.getNickname())
            );
            final HubUVFVault vault = new HubUVFVault(storage, bucket, prompt);
            vault.load(session, new HubVaultMetadataUVFProvider(vaultMetadata.toJSON(HubSession.coerce(session).getClient().getBasePath(), vaultId),
                    new HubVaultKeys(accessToken.key())));
            return vault;
        }
        catch(SecurityFailure e) {
            throw new VaultException(e.getMessage(), e);
        }
        catch(ApiException e) {
            if(HttpStatus.SC_FORBIDDEN == e.getCode()) {
                log.warn("Skip vault {} with insufficient permissions {}", id.getName(), e);
                throw new VaultUnlockCancelException(id);
            }
            throw new HubExceptionMappingService().map(e);
        }
    }
}
