/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Scheduler;
import ch.cyberduck.core.http.UserAgentHttpRequestInitializer;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.pool.SessionPool;
import ch.cyberduck.core.preferences.MemoryPreferences;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.threading.BackgroundActionState;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.Pair;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.core.CreateHubBookmarkAction;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.core.util.MockableFirstLoginDeviceSetupCallback;
import ch.iterate.hub.core.util.UnsecureHostPasswordStorePatched;
import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.hub.protocols.hub.HubCryptoVault;
import ch.iterate.hub.protocols.hub.HubProtocol;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.s3.S3AutoLoadVaultProtocol;
import ch.iterate.hub.protocols.s3.S3STSAutoLoadVaultProtocol;
import ch.iterate.hub.testsetup.model.HubTestSetupConfig;
import ch.iterate.hub.testsetup.model.HubTestSetupUserConfig;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class HubTestUtilities {
    private static final Logger log = LogManager.getLogger(HubTestUtilities.class.getName());

    public static final int SYNC_WAIT_SECS = 15;
    public static final int SYNC_INTERVAL_SECS = 10;

    public static Preferences preferences() {
        final Preferences preferences;
        PreferencesFactory.set(preferences = new MemoryPreferences() {
            @Override
            protected void configureLogging(final String level) {
                this.setDefault("logging.config", "log4j-test.xml");
                super.configureLogging(level);
            }
        });
        preferences.setProperty("cryptomator.vault.config.filename", "vault.uvf");
        preferences.setProperty("factory.vault.class", HubCryptoVault.class.getName());
        preferences.setProperty("factory.supportdirectoryfinder.class", ch.cyberduck.core.preferences.TemporarySupportDirectoryFinder.class.getName());
        preferences.setProperty("factory.passwordstore.class", UnsecureHostPasswordStorePatched.class.getName());
        preferences.setProperty("factory.firstlogindevicesetupcallback.class", MockableFirstLoginDeviceSetupCallback.class.getName());

        preferences.setProperty("hub.protocol.scheduler.period", SYNC_INTERVAL_SECS);
        preferences.setProperty("connection.unsecure.warning.http", false);
        try {
            preferences.setProperty("tmp.dir", Files.createTempDirectory("cipherduck_test_setup_alice").toString());
        }
        catch(IOException e) {
            log.error(e);
        }
        preferences.setLogging("debug");
        return preferences;
    }

    public static HubSession setupForUser(final HubTestSetupConfig hubTestSetupConfig, HubTestSetupUserConfig hubTestSetupUserConfig) throws BackgroundException, IOException {
        final String setupCode = hubTestSetupUserConfig.setupCode;
        final String username = hubTestSetupUserConfig.username;
        final String password = hubTestSetupUserConfig.password;
        final String hubURL = hubTestSetupConfig.hubURL();

        log.debug("/ start protocolSetup");
        ProtocolFactory factory = ProtocolFactory.get();
        factory.register(new S3AutoLoadVaultProtocol("PasswordGrant"), new S3STSAutoLoadVaultProtocol("PasswordGrant"), new HubProtocol("PasswordGrant", new CredentialsConfigurator() {
            @Override
            public Credentials configure(final Host host) {
                return new Credentials(username, password);
            }

            @Override
            public CredentialsConfigurator reload() {
                return this;
            }
        }));
        try {
            factory.register(new Local(AbstractHubTest.class.getResource("/S3 Hub.cyberduckprofile").toURI().getPath()));
            factory.register(new Local(AbstractHubTest.class.getResource("/S3 Hub STS.cyberduckprofile").toURI().getPath()));
        }
        catch(URISyntaxException e1) {
            throw new BackgroundException(e1);
        }
        log.debug("factory.load");
        factory.load();
        log.debug("\\ end protocolSetup");


        final FirstLoginDeviceSetupCallback proxy = new FirstLoginDeviceSetupCallback() {
            @Override
            public String displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) {
                return "firstLoginMockSetup";
            }

            @Override
            public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) {
                return new AccountKeyAndDeviceName().withAccountKey(setupCode).withDeviceName(String.format("firstLoginMockSetup %s", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                        .format(ZonedDateTime.now(ZoneId.of("Europe/Zurich")))));
            }
        };
        MockableFirstLoginDeviceSetupCallback.setProxy(proxy);

        final BookmarkCollection bookmarks = BookmarkCollection.defaultCollection();
        bookmarks.load();
        final HubTestController controller = new HubTestController();
        final Host hub = new CreateHubBookmarkAction(hubURL, bookmarks, controller).run();

        try {
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 bad code smell - we need to wait until added -> start background
            Thread.sleep(1500);
        }
        catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        // authorizeWithPassword takes credentials from bookmark
        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 bad code smell - intransparent is this safe? Is there a properer way?
        hub.withCredentials(new Credentials(username, password));
        final SessionPool pool = SessionPoolFactory.create(
                new LoginConnectionService(LoginCallbackFactory.get(controller), HostKeyCallbackFactory.get(controller, hub.getProtocol()), PasswordStoreFactory.get(), controller),
                controller, hub,
                new KeychainX509TrustManager(CertificateTrustCallbackFactory.get(controller), new DefaultTrustManagerHostnameCallback(hub), CertificateStoreFactory.get()),
                new KeychainX509KeyManager(CertificateIdentityCallbackFactory.get(controller), hub, CertificateStoreFactory.get()),
                new VaultRegistry.DisabledVaultRegistry());
        final HubSession session = (HubSession) pool.borrow(BackgroundActionState.running);
        final Scheduler scheduler = session.getFeature(Scheduler.class);
        if(scheduler != null) {
            log.info("Run repeating scheduler {}", scheduler);
            scheduler.repeat(new DisabledPasswordCallback());
        }
        return session;
    }

    public static Session<?> vaultLoginWithSharedOAuthCredentialsFromPasswordStore(final Host bookmark) throws BackgroundException {
        log.info(String.format("/ vaultLoginWithSharedOAuthCredentialsFromPasswordStore %s", bookmark));
        final Session<?> session = SessionFactory.create(bookmark, new DefaultX509TrustManager(), new DefaultX509KeyManager());
        // N.B. allow for injecting the OAuth token from hubsession into vault session through passwordstore, so we must not disable in testing!
        final LoginConnectionService login = new LoginConnectionService(
                new KeychainLoginService(PasswordStoreFactory.get()),
                new DisabledLoginCallback() {
                    @Override
                    public void warn(Host bookmark, String title, String message, String defaultButton, String cancelButton,
                                     String preference) {
                        // fail fast, otherwise the root cause disappears in log rotation (re-try login until stack overflow).
                        throw new RuntimeException(message);
                    }
                },
                new DisabledHostKeyCallback(),
                new DisabledProgressListener()
        );
        login.check(session, new DisabledCancelCallback());
        log.info(String.format("\\ vaultLoginWithSharedOAuthCredentialsFromPasswordStore %s", bookmark));
        return session;
    }

    public static ApiClient getAdminApiClient(final HubTestSetupConfig hubTestSetupConfig) throws IOException, ApiException {
        final ConfigDto config = new ConfigResourceApi(new ApiClient().setBasePath(hubTestSetupConfig.hubURL())).apiConfigGet();
        final PasswordTokenRequest request = new PasswordTokenRequest(new ApacheHttpTransport(), new GsonFactory(), new GenericUrl(config.getKeycloakTokenEndpoint()),
                hubTestSetupConfig.ADMIN().username, hubTestSetupConfig.ADMIN().password)
                .setClientAuthentication(new ClientParametersAuthentication("cryptomator", null))
                .setRequestInitializer(new UserAgentHttpRequestInitializer(new PreferencesUseragentProvider()));
        final String adminAccessToken = request.executeUnparsed().parseAs(OAuth2AuthorizationService.PermissiveTokenResponse.class).toTokenResponse().getAccessToken();
        final ApiClient adminApiClient = new ApiClient() {
            @Override
            protected void updateParamsForAuth(final String[] authNames, final List<Pair> queryParams, final Map<String, String> headerParams, final Map<String, String> cookieParams, final String payload, final String method, final URI uri) throws ApiException {
                super.updateParamsForAuth(authNames, queryParams, headerParams, cookieParams, payload, method, uri);
                headerParams.put("Authorization", String.format("Bearer %s", adminAccessToken));
            }
        };
        return adminApiClient.setBasePath(hubTestSetupConfig.hubURL());
    }
}
