/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Scheduler;
import ch.cyberduck.core.http.HttpConnectionPoolBuilder;
import ch.cyberduck.core.http.HttpSession;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2ErrorResponseInterceptor;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.shared.DelegatingSchedulerFeature;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.ssl.ThreadLocalHostnameDelegatingTrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.HubApiClient;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;

/**
 * Hub session is responsible for keeping OAuth tokens in Keychain valid as long as Cipherduck is running,
 * triggering OAuth flow upon expiry.
 * It provides a hub API client for accessing the hub REST API (managing authentication).
 */
public class HubSession extends HttpSession<HubApiClient> {
    private static final Logger log = LogManager.getLogger(HubSession.class);

    private final Scheduler<?> profiles = new HubStorageProfileSyncSchedulerService(this);
    private final Scheduler<?> vaults = new HubStorageVaultSyncSchedulerService(this);
    private final Scheduler<?> access = new HubGrantAccessSchedulerService(this);

    private final Scheduler<?> scheduler = new HubSchedulerService(Duration.ofSeconds(PreferencesFactory.get().getLong("hub.protocol.scheduler.period")).toMillis(),
            profiles, vaults, access);

    private OAuth2RequestInterceptor authorizationService;

    public HubSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    public OAuthTokens refresh() throws BackgroundException {
        return authorizationService.refresh();
    }

    @Override
    protected HubApiClient connect(final ProxyFinder proxy, final HostKeyCallback key, final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        final HttpClientBuilder configuration = builder.build(proxy, this, prompt);
        if(host.getProtocol().isOAuthConfigurable()) {
            // Setup authorization endpoint from configuration
            authorizationService = new OAuth2RequestInterceptor(configuration.build(), host,
                    host.getProtocol().getOAuthTokenUrl(),
                    host.getProtocol().getOAuthAuthorizationUrl(),
                    host.getProtocol().getOAuthClientId(),
                    host.getProtocol().getOAuthClientSecret(),
                    host.getProtocol().getOAuthScopes(),
                    true,
                    prompt)
                    .withFlowType(OAuth2AuthorizationService.FlowType.valueOf(host.getProtocol().getAuthorization()))
                    .withRedirectUri(host.getProtocol().getOAuthRedirectUrl());
            configuration.setServiceUnavailableRetryStrategy(new OAuth2ErrorResponseInterceptor(host, authorizationService));
            configuration.addInterceptorLast(authorizationService);
        }
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

    /**
     * Create hub session for URL.
     * The hub profile must already be installed and the OAuth tokens must be retrievable from keychain!
     * Used for hub API calls in <code>CreateVaultBookmarkAction</code> and <code>S3AutoLoadVaultSession</code>.
     *
     * @param hubURL     hub URL
     * @param controller conroller
     * @return hub session
     */
    public static HubSession createFromHubUrl(final String hubURL, final String username, final Controller controller) throws ApiException, BackgroundException {
        final ConfigDto hubConfig = new ConfigResourceApi(getHubApiClientBootstrapping(hubURL, controller)).apiConfigGet();
        final String hubUuid = hubConfig.getUuid();
        final Host bookmark = new HubProfileBookmarkService().makeHubBookmark(ProtocolFactory.get().forName(hubUuid), hubURL, hubUuid);
        // set username for OAuth sharing with username (findOAuthTokens)
        bookmark.getCredentials().setUsername(username);
        final X509TrustManager trust = new KeychainX509TrustManager(CertificateTrustCallbackFactory.get(controller), new DefaultTrustManagerHostnameCallback(bookmark), CertificateStoreFactory.get());
        final X509KeyManager key = new KeychainX509KeyManager(CertificateIdentityCallbackFactory.get(controller), bookmark, CertificateStoreFactory.get());
        final HubSession hubSession = new HubSession(bookmark, trust, key);
        final LoginConnectionService login = new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(), PasswordStoreFactory.get(), new DisabledProgressListener());
        login.check(hubSession, new DisabledCancelCallback());
        return hubSession;
    }

    /**
     * Create proxy-enabled client for calling hub REST API for bootstrapping via /api/config. Otherwise, use <code>createFromHubUrl</code>.
     *
     * @param hubURL hub url (without /api/ path
     * @return proxy-enabled client
     */
    public static HubApiClient getHubApiClientBootstrapping(String hubURL, final Controller controller) {
        // some reverse proxies do not handle double slashes well
        hubURL = hubURL.replaceAll("/$", "");
        final URI hubURI = URI.create(hubURL);
        final Host bookmark = new Host(new HubProtocol() {
            @Override
            public Scheme getScheme() {
                return Scheme.valueOf(hubURI.getScheme());
            }
        }, hubURI.getHost(), getPortFromHubURI(hubURI), hubURI.getPath());
        final X509TrustManager trust = new KeychainX509TrustManager(CertificateTrustCallbackFactory.get(controller), new DefaultTrustManagerHostnameCallback(bookmark), CertificateStoreFactory.get());
        final X509KeyManager key = new KeychainX509KeyManager(CertificateIdentityCallbackFactory.get(controller), bookmark, CertificateStoreFactory.get());
        final HttpConnectionPoolBuilder builder = new HttpConnectionPoolBuilder(bookmark, new ThreadLocalHostnameDelegatingTrustManager(trust, bookmark.getHostname()), key, ProxyFactory.get());
        final HttpClientBuilder configuration = builder.build(ProxyFactory.get(), new DisabledTranscriptListener(), new DisabledLoginCallback());
        return new HubApiClient(bookmark, configuration.build());
    }

    @Override
    protected void logout() {
        scheduler.shutdown(false);
        client.getHttpClient().close();
    }

    public static int getPortFromHubURI(final URI hubURI) {
        return -1 != hubURI.getPort() ? hubURI.getPort() : Scheme.valueOf(hubURI.getScheme()).getPort();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T _getFeature(final Class<T> type) {
        // ListService, Read (used by RemoteProfileFinder) and ComparisonService (see ProfilesSynchronizeWorker.filter(), https://github.com/iterate-ch/cyberduck/pull/15602/files)
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
            return (T) new DelegatingSchedulerFeature(
                    profiles,
                    vaults,
                    access
            );
        }
        return super._getFeature(type);
    }
}
