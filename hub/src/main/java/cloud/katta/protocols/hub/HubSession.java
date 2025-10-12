/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Copy;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Location;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Scheduler;
import ch.cyberduck.core.features.Timestamp;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.http.CustomServiceUnavailableRetryStrategy;
import ch.cyberduck.core.http.ExecutionCountServiceUnavailableRetryStrategy;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.io.StatusOutputStream;
import ch.cyberduck.core.io.StreamListener;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2ErrorResponseInterceptor;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.synchronization.ComparisonService;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cloud.katta.client.ApiException;
import cloud.katta.client.HubApiClient;
import cloud.katta.client.api.ConfigResourceApi;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.ConfigDto;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.protocols.hub.serializer.HubConfigDtoDeserializer;
import cloud.katta.workflows.DeviceKeysServiceImpl;
import cloud.katta.workflows.UserKeysServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;

/**
 * Providing Katta Server client for accessing its REST API
 */
public class HubSession extends HttpSession<HubApiClient> {
    private static final Logger log = LogManager.getLogger(HubSession.class);

    private final HostPasswordStore keychain = PasswordStoreFactory.get();

    /**
     * Periodically grant vault access to users
     */
    private final Scheduler<?> access = new HubGrantAccessSchedulerService(this, keychain);

    /**
     * Interceptor for OpenID connect flow
     */
    private OAuth2RequestInterceptor authorizationService;

    private UserDto me;
    private ConfigDto config;
    private UserKeys userKeys;
    private AttributedList<Path> vaults;

    public HubSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    public static HubSession coerce(final Session<?> session) {
        return (HubSession) session;
    }

    @Override
    protected HubApiClient connect(final ProxyFinder proxy, final HostKeyCallback key,
                                   final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        final HttpClientBuilder configuration = builder.build(proxy, this, prompt);
        final Protocol bundled = host.getProtocol();
        if(bundled.isBundled()) {
            // Use REST API for bootstrapping via /api/config
            final HubApiClient client = new HubApiClient(host, configuration.build());
            try {
                // Obtain OAuth configuration
                config = new ConfigResourceApi(client).apiConfigGet();
                final int minHubApiLevel = HostPreferencesFactory.get(host).getInteger("cloud.katta.min_api_level");
                final Integer apiLevel = config.getApiLevel();
                if(apiLevel == null || apiLevel < minHubApiLevel) {
                    final String detail = String.format("Client requires API level at least %s, found %s, for hub %s", minHubApiLevel, apiLevel, host);
                    log.error(detail);
                    throw new InteroperabilityException(LocaleFactory.localizedString("Login failed", "Credentials"), detail);
                }

                final String hubId = config.getUuid();
                log.debug("Configure bookmark with id {}", hubId);
                host.setUuid(hubId);
                final Profile profile = new Profile(bundled, new HubConfigDtoDeserializer(config));
                log.debug("Apply profile {} to bookmark {}", profile, host);
                host.setProtocol(profile);
            }
            catch(ApiException e) {
                throw new HubExceptionMappingService().map(e);
            }
            finally {
                client.getHttpClient().close();
            }
        }
        // Setup authorization endpoint from configuration
        authorizationService = new OAuth2RequestInterceptor(configuration.build(), host,
                host.getProtocol().getOAuthTokenUrl(),
                host.getProtocol().getOAuthAuthorizationUrl(),
                host.getProtocol().getOAuthClientId(),
                host.getProtocol().getOAuthClientSecret(),
                host.getProtocol().getOAuthScopes(),
                host.getProtocol().isOAuthPKCE(), prompt)
                .withFlowType(OAuth2AuthorizationService.FlowType.valueOf(host.getProtocol().getAuthorization()))
                .withRedirectUri(host.getProtocol().getOAuthRedirectUrl());
        configuration.setServiceUnavailableRetryStrategy(new CustomServiceUnavailableRetryStrategy(host,
                new ExecutionCountServiceUnavailableRetryStrategy(new OAuth2ErrorResponseInterceptor(host, authorizationService))));
        configuration.addInterceptorLast(authorizationService);
        return new HubApiClient(host, configuration.build());
    }

    @Override
    public void login(final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        final Credentials credentials = authorizationService.validate();
        try {
            // Set username from OAuth ID Token for saving in keychain
            credentials.setUsername(JWT.decode(credentials.getOauth().getIdToken()).getSubject());
        }
        catch(JWTDecodeException e) {
            log.warn("Failure {} decoding JWT {}", e, credentials.getOauth().getIdToken());
            throw new LoginCanceledException(e);
        }
        try {
            me = new UsersResourceApi(client).apiUsersMeGet(true, false);
            log.debug("Retrieved user {}", me);
            // Ensure device key is available
            final DeviceSetupCallback setup = prompt.getFeature(DeviceSetupCallback.class);
            log.debug("Configured with setup prompt {}", setup);
            userKeys = this.pair(setup);
            final List<StorageProfileDto> storageProfileDtos = new StorageProfileResourceApi(client).apiStorageprofileGet(false);
            for(StorageProfileDto storageProfileDto : storageProfileDtos) {
                final StorageProfileDtoWrapper storageProfile = StorageProfileDtoWrapper.coerce(storageProfileDto);
                log.debug("Read storage profile {}", storageProfile);
                switch(storageProfile.getProtocol()) {
                    case S3:
                    case S3_STS:
                        final ProtocolFactory protocols = ProtocolFactory.get();
                        final Profile profile = new HubAwareProfile(this, protocols.forType(protocols.find(ProtocolFactory.BUNDLED_PROFILE_PREDICATE), Protocol.Type.s3),
                                config, storageProfile);
                        log.debug("Register profile {}", profile);
                        protocols.register(profile);
                        break;
                    default:
                        throw new InteroperabilityException(String.format("Unsupported storage configuration %s", storageProfile.getProtocol().name()));
                }
            }
            // Ensure vaults are registered
            try {
                vaults = new HubVaultListService(this, prompt).list(Home.root(), new DisabledListProgressListener());
            }
            finally {
                log.debug("Destroyed user keys {}", userKeys);
                // Short-lived
                userKeys.destroy();
            }
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    private UserKeys pair(final DeviceSetupCallback setup) throws BackgroundException {
        try {
            final DeviceKeys deviceKeys = new DeviceKeysServiceImpl(keychain).getOrCreateDeviceKeys(host, setup);
            log.debug("Retrieved device keys {}", deviceKeys);
            final UserKeys userKeys = new UserKeysServiceImpl(this, keychain).getOrCreateUserKeys(host, me, deviceKeys, setup);
            log.debug("Retrieved user keys {}", userKeys);
            return userKeys;
        }
        catch(SecurityFailure e) {
            // Repeat until canceled by user
            return this.pair(setup);
        }
        catch(AccessException e) {
            throw new ConnectionCanceledException(e);
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    protected void logout() {
        access.shutdown(false);
        client.getHttpClient().close();
    }

    /**
     *
     * @return Null prior login
     */
    public UserDto getMe() {
        return me;
    }

    /**
     *
     * @return Null when not connected
     */
    public ConfigDto getConfig() {
        return config;
    }

    /**
     *
     * @return Destroyed keys after login
     */
    public UserKeys getUserKeys() {
        return userKeys;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(final Class<T> type) {
        if(type == ListService.class) {
            return (T) (ListService) (directory, listener) -> vaults;
        }
        if(type == Scheduler.class) {
            return (T) access;
        }
        if(type == Home.class) {
            return (T) (Home) Home::root;
        }
        if(type == AttributesFinder.class) {
            return (T) (AttributesFinder) (f, l) -> f.attributes();
        }
        if(type == Location.class) {
            return (T) new HubStorageLocationService(this);
        }
        if(type == Find.class) {
            return (T) (Find) (file, listener) -> new SimplePathPredicate(registry.find(HubSession.this, file).getHome()).test(file);
        }
        if(type == Read.class) {
            return (T) new Read() {
                @Override
                public InputStream read(final Path file, final TransferStatus status, final ConnectionCallback callback) throws BackgroundException {
                    log.warn("Deny read access to {}", file);
                    throw new UnsupportedException().withFile(file);
                }

                @Override
                public void preflight(final Path file) throws BackgroundException {
                    throw new UnsupportedException().withFile(file);
                }
            };
        }
        if(type == Write.class) {
            return (T) new Write<Void>() {
                @Override
                public StatusOutputStream<Void> write(final Path file, final TransferStatus status, final ConnectionCallback callback) throws BackgroundException {
                    log.warn("Deny write access to {}", file);
                    throw new UnsupportedException().withFile(file);
                }

                @Override
                public void preflight(final Path file) throws BackgroundException {
                    throw new UnsupportedException().withFile(file);
                }
            };
        }
        if(type == Touch.class) {
            return (T) new Touch<Void>() {
                @Override
                public Path touch(final Write<Void> writer, final Path file, final TransferStatus status) throws BackgroundException {
                    log.warn("Deny write access to {}", file);
                    throw new UnsupportedException().withFile(file);
                }

                @Override
                public void preflight(final Path workdir, final String filename) throws BackgroundException {
                    throw new UnsupportedException().withFile(workdir);
                }
            };
        }
        if(type == Directory.class) {
            return (T) new Directory<Void>() {
                @Override
                public Path mkdir(final Write<Void> writer, final Path folder, final TransferStatus status) throws BackgroundException {
                    log.warn("Deny write access to {}", folder);
                    throw new UnsupportedException().withFile(folder);
                }

                @Override
                public void preflight(final Path workdir, final String filename) throws BackgroundException {
                    throw new UnsupportedException().withFile(workdir);
                }
            };
        }
        if(type == Move.class) {
            return (T) new Move() {
                @Override
                public Path move(final Path source, final Path target, final TransferStatus status, final Delete.Callback delete, final ConnectionCallback prompt) throws BackgroundException {
                    log.warn("Deny write access to {}", source);
                    throw new UnsupportedException().withFile(source);
                }

                @Override
                public void preflight(final Path source, final Optional<Path> target) throws BackgroundException {
                    throw new UnsupportedException().withFile(source);
                }
            };
        }
        if(type == Copy.class) {
            return (T) new Copy() {
                @Override
                public Path copy(final Path source, final Path target, final TransferStatus status, final ConnectionCallback prompt, final StreamListener listener) throws BackgroundException {
                    log.warn("Deny write access to {}", source);
                    throw new UnsupportedException().withFile(source);
                }

                @Override
                public void preflight(final Path source, final Optional<Path> target) throws BackgroundException {
                    throw new UnsupportedException().withFile(source);
                }
            };
        }
        if(type == Delete.class) {
            return (T) new Delete() {
                @Override
                public void delete(final Map<Path, TransferStatus> files, final PasswordCallback prompt, final Callback callback) throws BackgroundException {
                    log.warn("Deny write access to {}", files);
                    throw new UnsupportedException();
                }

                @Override
                public void preflight(final Path file) throws BackgroundException {
                    throw new UnsupportedException().withFile(file);
                }
            };
        }
        if(type == Timestamp.class) {
            return (T) (Timestamp) (file, status) -> {
                throw new UnsupportedException().withFile(file);
            };
        }
        if(type == ComparisonService.class) {
            return (T) new HubVaultStorageAwareComparisonService(this);
        }
        if(type == CredentialsConfigurator.class) {
            return (T) new HubOAuthTokensCredentialsConfigurator(keychain, host);
        }
        return super._getFeature(type);
    }
}
