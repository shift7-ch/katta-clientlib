/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import ch.cyberduck.core.*;
import ch.cyberduck.core.preferences.MemoryPreferences;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.profiles.LocalProfilesFinder;
import ch.cyberduck.core.serviceloader.AnnotationAutoServiceLoader;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.vault.VaultRegistryFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubUVFVault;
import cloud.katta.protocols.hub.HubVaultRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@HubIntegrationTest
public abstract class AbstractHubTest {

    public static final HubTestConfig.Setup.DockerConfig LOCAL_DOCKER_CONFIG = new HubTestConfig.Setup.DockerConfig(
            "/docker-compose-minio-localhost-hub.yml",
            "/.local.env",
            "local");

    private static final Properties LOCAL_PROPERTIES = new Properties();

    static {
        try (InputStream in = Objects.requireNonNull(AbstractHubTest.class.getResourceAsStream(LOCAL_DOCKER_CONFIG.envFile))) {
            LOCAL_PROPERTIES.load(in);
        }
        catch(IOException e) {
            fail(e);
        }
    }

    /**
     * hub, Keycloak, MinIO, localstack started via testcontainers+docker-compose.
     */
    public static final HubTestConfig.Setup LOCAL_TEST_CONFIG = new HubTestConfig.Setup()
            .withHubURL(LOCAL_PROPERTIES.getProperty("HUB_URL"))
            .withUserConfig(new HubTestConfig.Setup.UserConfig(
                    LOCAL_PROPERTIES.getProperty("HUB_USER"),
                    LOCAL_PROPERTIES.getProperty("HUB_PASSWORD"),
                    staticSetupCode()))
            .withAdminConfig(new HubTestConfig.Setup.UserConfig(
                    LOCAL_PROPERTIES.getProperty("HUB_ADMIN_USER"),
                    LOCAL_PROPERTIES.getProperty("HUB_ADMIN_PASSWORD"),
                    staticSetupCode()))
            .withDockerConfig(LOCAL_DOCKER_CONFIG);

    private static final Function<HubTestConfig.VaultSpec, Arguments> prepareArgumentLocal = vs -> Arguments.of(Named.of(
            String.format("%s %s", vs.storageProfileName, LOCAL_TEST_CONFIG.hubURL),
            new HubTestConfig(LOCAL_TEST_CONFIG, vs)));

    public static final Arguments LOCAL_MINIO_STATIC = prepareArgumentLocal.apply(new HubTestConfig.VaultSpec(
            "MinIO static", "71B910E0-2ECC-46DE-A871-8DB28549677E",
            LOCAL_PROPERTIES.getProperty("MINIO_USER_ACCESS_KEY"),
            LOCAL_PROPERTIES.getProperty("MINIO_USER_SECRET_KEY"),
            "us-east-1"));
    public static final Arguments LOCAL_MINIO_STS = prepareArgumentLocal.apply(new HubTestConfig.VaultSpec(
            "MinIO STS", "732D43FA-3716-46C4-B931-66EA5405EF1C",
            null, null, "eu-central-1"));


    public static final HubTestConfig.Setup.DockerConfig HYBRID_DOCKER_CONFIG = new HubTestConfig.Setup.DockerConfig(
            "/docker-compose-minio-localhost-hub.yml",
            "/.hybrid.env",
            "hybrid"
    );

    private static final Properties HYBRID_PROPERTIES = new Properties();

    static {
        try (InputStream in = Objects.requireNonNull(AbstractHubTest.class.getResourceAsStream(HYBRID_DOCKER_CONFIG.envFile))) {
            HYBRID_PROPERTIES.load(in);
        }
        catch(IOException e) {
            fail(e);
        }
    }

    /**
     * local hub (testcontainers+docker-compose) against AWS/MinIO/Keycloak remote.
     */
    public static final HubTestConfig.Setup HYBRID_TEST_CONFIG = new HubTestConfig.Setup()
            .withHubURL(HYBRID_PROPERTIES.getProperty("HUB_URL"))
            .withUserConfig(new HubTestConfig.Setup.UserConfig(
                    HYBRID_PROPERTIES.getProperty("HUB_USER"),
                    HYBRID_PROPERTIES.getProperty("HUB_PASSWORD"),
                    staticSetupCode()))
            .withAdminConfig(new HubTestConfig.Setup.UserConfig(
                    HYBRID_PROPERTIES.getProperty("HUB_ADMIN_USER"),
                    HYBRID_PROPERTIES.getProperty("HUB_ADMIN_PASSWORD"),
                    staticSetupCode()))
            .withDockerConfig(HYBRID_DOCKER_CONFIG);

    private static final Function<HubTestConfig.VaultSpec, Arguments> prepareArgumentsHybrid = vs -> Arguments.of(Named.of(
            String.format("%s %s", vs.storageProfileName, HYBRID_TEST_CONFIG.hubURL),
            new HubTestConfig(HYBRID_TEST_CONFIG, vs)));


    public static final Arguments HYBRID_MINIO_STATIC = prepareArgumentsHybrid.apply(new HubTestConfig.VaultSpec(
            "MinIO static", "71B910E0-2ECC-46DE-A871-8DB285496779",
            HYBRID_PROPERTIES.getProperty("MINIO_USER_ACCESS_KEY"),
            HYBRID_PROPERTIES.getProperty("MINIO_USER_SECRET_KEY"),
            "us-east-1"
    ));
    public static final Arguments HYBRID_MINIO_STS = prepareArgumentsHybrid.apply(new HubTestConfig.VaultSpec(
            "MinIO STS", "732D43FA-3716-46C4-B931-66EA5405EF19",
            null, null, "eu-central-1"
    ));

    public static final Arguments HYBRID_AWS_STATIC = prepareArgumentsHybrid.apply(new HubTestConfig.VaultSpec(
            "AWS static", "72736C19-283C-49D3-80A5-AB74B5202549",
            HYBRID_PROPERTIES.getProperty("AWS_USER_ACCESS_KEY"),
            HYBRID_PROPERTIES.getProperty("AWS_USER_SECRET_KEY"),
            "eu-north-1"
    ));
    public static final Arguments HYBRID_AWS_STS = prepareArgumentsHybrid.apply(new HubTestConfig.VaultSpec(
            "AWS STS", "844BD517-96D4-4787-BCFA-238E103149F9",
            null, null, "eu-west-1"
    ));

    @BeforeEach
    public void preferences() throws IOException {
        final Preferences preferences;
        PreferencesFactory.set(preferences = new MemoryPreferences() {
            @Override
            protected void configureLogging(final String level) {
                this.setDefault("logging.config", "log4j-test.xml");
                super.configureLogging(level);
            }
        });
        preferences.setProperty("cryptomator.vault.config.filename", "vault.uvf");
        preferences.setProperty("cryptomator.vault.autodetect", "false");
        preferences.setProperty("factory.vault.class", HubUVFVault.class.getName());
        preferences.setProperty("factory.supportdirectoryfinder.class", ch.cyberduck.core.preferences.TemporarySupportDirectoryFinder.class.getName());
        preferences.setProperty("factory.passwordstore.class", UnsecureHostPasswordStore.class.getName());
        preferences.setProperty("factory.vaultregistry.class", HubVaultRegistry.class.getName());

        preferences.setProperty("oauth.handler.scheme", "katta");
        preferences.setProperty("hub.protocol.scheduler.period", 30);
        preferences.setProperty("cryptomator.vault.autodetect", false);
        preferences.setProperty("connection.unsecure.warning.http", false);
        preferences.setProperty("cloud.katta.min_api_level", 4);

        preferences.setProperty("tmp.dir", Files.createTempDirectory("cipherduck_test_setup_alice").toString());
    }

    private static String staticSetupCode() {
        // set to some value to have staticSetupCode instead of random string in attended local test setup.
        return "setupcode";
    }

    protected static HubSession setupConnection(final HubTestConfig config) throws Exception {
        final ProtocolFactory factory = ProtocolFactory.get();
        // Register parent protocol definitions
        for(Protocol p : new AnnotationAutoServiceLoader<Protocol>().load(Protocol.class)) {
            factory.register(p);
        }
        // Load bundled profiles
        factory.load(new LocalProfilesFinder(factory, new Local(AbstractHubTest.class.getResource("/").toURI().getPath())));
        assertNotNull(factory.forName("hub:katta"));

        final Host hub = new HostParser(factory).get(config.setup.hubURL).setCredentials(new Credentials(config.setup.userConfig.username, config.setup.userConfig.password));
        final HubSession session = (HubSession) SessionFactory.create(hub, new DefaultX509TrustManager(), new DefaultX509KeyManager())
                .withRegistry(VaultRegistryFactory.get(new DisabledPasswordCallback()));
        final LoginConnectionService login = new LoginConnectionService(loginCallback(config), new DisabledHostKeyCallback(),
                PasswordStoreFactory.get(), new DisabledProgressListener());
        login.check(session, CancelCallback.noop);
        return session;
    }

    protected static LoginCallback loginCallback(HubTestConfig config) {
        return new DisabledLoginCallback() {
            @Override
            public Credentials prompt(final Host bookmark, final String username, final String title, final String reason, final LoginOptions options) {
                return new Credentials(config.vault.username, config.vault.password);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getFeature(final Class<T> type) {
                if(DeviceSetupCallback.class == type) {
                    return (T) deviceSetupCallback(config.setup);
                }
                return null;
            }
        };
    }

    protected static DeviceSetupCallback deviceSetupCallback(HubTestConfig.Setup setup) {
        return new DeviceSetupCallback() {
            @Override
            public AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(final Host bookmark, final String accountKey) {
                return new AccountKeyAndDeviceName(setup.userConfig.setupCode, String.format("%s %s", AccountKeyAndDeviceName.COMPUTER_NAME, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                        .format(ZonedDateTime.now(ZoneId.of("Europe/Zurich")))));
            }

            @Override
            public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark) {
                return new AccountKeyAndDeviceName(setup.userConfig.setupCode,
                        String.format("%s %s", AccountKeyAndDeviceName.COMPUTER_NAME, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                                .format(ZonedDateTime.now(ZoneId.of("Europe/Zurich")))));
            }

            @Override
            public String generateAccountKey() {
                return staticSetupCode();
            }
        };
    }
}

