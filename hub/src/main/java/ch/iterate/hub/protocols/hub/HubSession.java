/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Scheduler;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2ErrorResponseInterceptor;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.shared.DelegatingSchedulerFeature;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.HubApiClient;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.protocols.hub.serializer.HubConfigDtoDeserializer;
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

    /**
     * Key in bookmark to reference UUID of configuration from API
     */
    public static final String HUB_UUID = "hub.uuid";

    /**
     * Read storage configurations from API
     */
    private final Scheduler<?> profiles = new HubStorageProfileSyncSchedulerService(this);
    /**
     * Read available vaults from API
     */
    private final Scheduler<?> vaults = new HubStorageVaultSyncSchedulerService(this);
    /**
     * Periodically grant vault access to users
     */
    private final Scheduler<?> access = new HubGrantAccessSchedulerService(this);

    private final Scheduler<?> scheduler = new HubSchedulerService(Duration.ofSeconds(
            new HostPreferences(host).getLong("hub.protocol.scheduler.period")).toMillis(), profiles, vaults, access);

    /**
     * Interceptor for OpenID connect flow
     */
    private OAuth2RequestInterceptor authorizationService;

    public HubSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
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
            // Ask for account key (setup code) and device name
            new UserKeysServiceImpl(this).getUserKeys(host, FirstLoginDeviceSetupCallbackFactory.get());
            // Fetch storage configuration once
            try {
                scheduler.execute(prompt).get();
            }
            catch(InterruptedException e) {
                throw new ConnectionCanceledException(e);
            }
            catch(ExecutionException e) {
                if(e.getCause() instanceof BackgroundException) {
                    throw (BackgroundException) e.getCause();
                }
                throw new BackgroundException(e.getCause());
            }
        }
        catch(AccessException | SecurityFailure e) {
            throw new InteroperabilityException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    protected void logout() {
        scheduler.shutdown(false);
        client.getHttpClient().close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(final Class<T> type) {
        if(type == ListService.class) {
            return (T) new HubStorageProfileListService(this);
        }
        if(type == Read.class) {
            return (T) new HubStorageProfileListService(this);
        }
        if(type == Find.class) {
            return (T) new HubStorageProfileListService(this).new StorageProfileFindFeature();
        }
        if(type == AttributesFinder.class) {
            return (T) new HubStorageProfileListService(this).new StorageProfileAttributesFinder();
        }
        if(type == Scheduler.class) {
            return (T) scheduler;
        }
        return super._getFeature(type);
    }
}
