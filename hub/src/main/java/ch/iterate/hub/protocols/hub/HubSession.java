/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Scheduler;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2ErrorResponseInterceptor;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.HubApiClient;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.core.DeviceSetupCallback;
import ch.iterate.hub.core.DeviceSetupCallbackFactory;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.protocols.hub.serializer.HubConfigDtoDeserializer;
import ch.iterate.hub.workflows.DeviceKeysServiceImpl;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;

/**
 * Providing Katta Server client for accessing its REST API
 */
public class HubSession extends HttpSession<HubApiClient> {
    private static final Logger log = LogManager.getLogger(HubSession.class);

    private final HostPasswordStore keychain = PasswordStoreFactory.get();
    private final ProtocolFactory protocols = ProtocolFactory.get();

    /**
     * Periodically grant vault access to users
     */
    private final Scheduler<?> access = new HubGrantAccessSchedulerService(this, keychain);

    private final HubVaultRegistry registry = new HubVaultRegistry();

    private final HubVaultListService vaults = new HubVaultListService(protocols,
            this, trust, key, registry, keychain);

    /**
     * Interceptor for OpenID connect flow
     */
    private OAuth2RequestInterceptor authorizationService;
    private UserDto me;

    public HubSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    @Override
    public Session<?> withRegistry(final VaultRegistry ignored) {
        return super.withRegistry(registry);
    }

    public HubVaultRegistry getRegistry() {
        return registry;
    }

    @Override
    protected HubApiClient connect(final ProxyFinder proxy, final HostKeyCallback key,
                                   final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        final HttpClientBuilder configuration = builder.build(proxy, this, prompt);
        if(host.getProtocol().isBundled()) {
            // Use REST API for bootstrapping via /api/config
            final HubApiClient client = new HubApiClient(host, configuration.build());
            try {
                // Obtain OAuth configuration
                final ConfigDto configDto = new ConfigResourceApi(client).apiConfigGet();
                final String hubId = configDto.getUuid().toString();
                log.debug("Configure bookmark with id {}", hubId);
                host.setUuid(hubId);
                final Profile profile = new Profile(host.getProtocol(), new HubConfigDtoDeserializer(configDto));
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
        configuration.setServiceUnavailableRetryStrategy(new OAuth2ErrorResponseInterceptor(host, authorizationService));
        configuration.addInterceptorLast(authorizationService);
        return new HubApiClient(host, configuration.build());
    }

    @Override
    public void login(final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        final DeviceSetupCallback setup = DeviceSetupCallbackFactory.get();
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
            me = new UsersResourceApi(client).apiUsersMeGet(true);
            log.debug("Retrieved user {}", me);
            final UserKeys userKeys = new UserKeysServiceImpl(this).getOrCreateUserKeys(host, me,
                    new DeviceKeysServiceImpl(keychain).getOrCreateDeviceKeys(host, setup), setup);
            log.debug("Retrieved user keys {}", userKeys);
            // Ensure vaults are registered
            vaults.list(Home.ROOT, new DisabledListProgressListener());
        }
        catch(SecurityFailure e) {
            throw new InteroperabilityException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
        catch(AccessException e) {
            throw new ConnectionCanceledException(e);
        }
    }

    @Override
    protected void logout() {
        access.shutdown(false);
        client.getHttpClient().close();
    }

    public UserDto getMe() {
        return me;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(final Class<T> type) {
        if(type == ListService.class) {
            return (T) vaults;
        }
        if(type == Scheduler.class) {
            return (T) access;
        }
        return host.getProtocol().getFeature(type);
    }
}
