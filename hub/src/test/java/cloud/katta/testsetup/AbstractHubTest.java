/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import ch.cyberduck.core.*;
import ch.cyberduck.core.preferences.MemoryPreferences;
import ch.cyberduck.core.preferences.Preferences;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.profiles.LocalProfilesFinder;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.vault.VaultRegistryFactory;
import ch.cyberduck.test.VaultTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.core.util.MockableDeviceSetupCallback;
import cloud.katta.model.AccountKeyAndDeviceName;
import cloud.katta.protocols.hub.HubProtocol;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubUVFVault;
import cloud.katta.protocols.hub.HubVaultRegistry;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;

@HubIntegrationTest
public abstract class AbstractHubTest extends VaultTest {

    static {
        // VaultTest is Junit 4 with @BeforeClass annotation, call statically in Jupiter setup.
        credentials();
    }

    private static final HubTestConfig.VaultSpec minioSTSVaultConfig = new HubTestConfig.VaultSpec("MinIO STS", "732D43FA-3716-46C4-B931-66EA5405EF1C",
            null, null, null, null);
    private static final HubTestConfig.VaultSpec minioStaticVaultConfig = new HubTestConfig.VaultSpec("MinIO static", "71B910E0-2ECC-46DE-A871-8DB28549677E",
            "handmade", "minioadmin", "minioadmin", null);
    private static final HubTestConfig.VaultSpec awsSTSVaultConfig = new HubTestConfig.VaultSpec("AWS STS", "844BD517-96D4-4787-BCFA-238E103149F6",
            null, null, null, null);
    private static final HubTestConfig.VaultSpec awsStaticVaultConfig = new HubTestConfig.VaultSpec("AWS static", "72736C19-283C-49D3-80A5-AB74B5202543",
            "cipherststest",
            PROPERTIES.get(String.format("%s.user", "cipherduck.AWS_CIPHERSTSTEST")),
            PROPERTIES.get(String.format("%s.password", "cipherduck.AWS_CIPHERSTSTEST")),
            null
    );

    /**
     * Attended local only: hub, Keycloak, minio started manually.
     */
    private static final HubTestConfig.Setup ATTENDED_LOCAL_ONLY = new HubTestConfig.Setup()
            .withHubURL("http://localhost:8080")
            .withAdminConfig(new HubTestConfig.Setup.UserConfig("admin", "admin", staticSetupCode()))
            .withUserConfig(new HubTestConfig.Setup.UserConfig("alice", "asd", staticSetupCode()));
    private static final Function<HubTestConfig.VaultSpec, Arguments> argumentAttendedLocalOnly = vs -> Arguments.of(Named.of(
            String.format("%s %s (Bucket %s)", vs.storageProfileName, ATTENDED_LOCAL_ONLY.hubURL, vs.bucketName),
            new HubTestConfig(ATTENDED_LOCAL_ONLY, vs)));
    public static final Arguments minioStaticAttendedLocalOnly = argumentAttendedLocalOnly.apply(minioStaticVaultConfig);
    public static final Arguments minioSTSAttendedLocalOnly = argumentAttendedLocalOnly.apply(minioSTSVaultConfig);

    /**
     * Unattended local only: hub, Keycloak, MinIO started via docker-compose.
     */
    public static final HubTestConfig.Setup UNATTENDED_LOCAL_ONLY = new HubTestConfig.Setup()
            .withHubURL("http://localhost:8280")
            .withUserConfig(new HubTestConfig.Setup.UserConfig("alice", "asd", staticSetupCode()))
            .withAdminConfig(new HubTestConfig.Setup.UserConfig("admin", "admin", staticSetupCode()))
            .withDockerConfig(new HubTestConfig.Setup.DockerConfig("/docker-compose-minio-localhost-hub.yml", 8380, 9100, 9101, 8280));
    private static final Function<HubTestConfig.VaultSpec, Arguments> argumentUnattendedLocalOnly = vs -> Arguments.of(Named.of(
            String.format("%s %s (Bucket %s)", vs.storageProfileName, UNATTENDED_LOCAL_ONLY.hubURL, vs.bucketName),
            new HubTestConfig(UNATTENDED_LOCAL_ONLY, vs)));
    public static final Arguments minioStaticUnattendedLocalOnly = argumentUnattendedLocalOnly.apply(minioStaticVaultConfig);
    public static final Arguments minioSTSUnattendedLocalOnly = argumentUnattendedLocalOnly.apply(minioSTSVaultConfig);

    /**
     * Unattended Keycloak Testing: local hub (docker-compose) against AWS/MinIO/Keycloak remote
     */
    public static final HubTestConfig.Setup UNATTENDED_LOCAL_KEYCLOAK_TESTING = new HubTestConfig.Setup()
            // N.B. port needs to match dev-realm.json as injected by hub/pom.xml
            .withHubURL("http://localhost:8280")
            .withUserConfig(new HubTestConfig.Setup.UserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    staticSetupCode()))
            .withAdminConfig(new HubTestConfig.Setup.UserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    staticSetupCode()))
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 improvement: no need to start keycloak in this setting
            .withDockerConfig(new HubTestConfig.Setup.DockerConfig("/docker-compose-minio-localhost-hub.yml", 8380, 9100, 9101, 8280));

    /**
     * Attended Keycloak Testing: local hub, local MinIO, remote AWS, remote Keycloak.
     */
    public static final HubTestConfig.Setup ATTENDED_LOCAL_KEYCLOAK_TESTING = new HubTestConfig.Setup()
            .withHubURL("http://localhost:8080")
            .withUserConfig(new HubTestConfig.Setup.UserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    staticSetupCode()))
            .withAdminConfig(new HubTestConfig.Setup.UserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    staticSetupCode()));
    private static final Function<HubTestConfig.VaultSpec, Arguments> argumentAttendedLocalKeycloadkDev = vs -> Arguments.of(Named.of(
            String.format("%s %s (Bucket %s)", vs.storageProfileName, ATTENDED_LOCAL_KEYCLOAK_TESTING.hubURL, vs.bucketName),
            new HubTestConfig(ATTENDED_LOCAL_KEYCLOAK_TESTING, vs)));
    public static final Arguments awsStaticAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(awsStaticVaultConfig);
    public static final Arguments awsSTSAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(awsSTSVaultConfig);
    public static final Arguments minioStaticAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(minioStaticVaultConfig);
    public static final Arguments minioSTSAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(minioSTSVaultConfig);

    @BeforeAll
    public static void preferences() throws IOException {
        final Preferences preferences;
        PreferencesFactory.set(preferences = new MemoryPreferences() {
            @Override
            protected void configureLogging(final String level) {
                this.setDefault("logging.config", "log4j-test.xml");
                super.configureLogging(level);
            }
        });
        preferences.setLogging("debug");
        preferences.setProperty("cryptomator.vault.config.filename", "vault.uvf");
        preferences.setProperty("cryptomator.vault.autodetect", "false");
        preferences.setProperty("factory.vault.class", HubUVFVault.class.getName());
        preferences.setProperty("factory.supportdirectoryfinder.class", ch.cyberduck.core.preferences.TemporarySupportDirectoryFinder.class.getName());
        preferences.setProperty("factory.passwordstore.class", UnsecureHostPasswordStore.class.getName());
        preferences.setProperty("factory.devicesetupcallback.class", MockableDeviceSetupCallback.class.getName());
        preferences.setProperty("factory.vaultregistry.class", HubVaultRegistry.class.getName());

        preferences.setProperty("oauth.handler.scheme", "katta");
        preferences.setProperty("hub.protocol.scheduler.period", 30);
        preferences.setProperty("cryptomator.vault.autodetect", false);
        preferences.setProperty("connection.unsecure.warning.http", false);

        preferences.setProperty("tmp.dir", Files.createTempDirectory("cipherduck_test_setup_alice").toString());
    }

    private static String staticSetupCode() {
        // set to some value to have staticSetupCode instead of random string in attended local test setup.
        return "setupcode";
    }

    protected static HubSession setupConnection(final HubTestConfig.Setup setup) throws Exception {
        final ProtocolFactory factory = ProtocolFactory.get();
        // Register parent protocol definitions
        factory.register(
                new HubProtocol(),
                new S3AssumeRoleProtocol("PasswordGrant")
        );
        // Load bundled profiles
        factory.load(new LocalProfilesFinder(factory, new Local(AbstractHubTest.class.getResource("/").toURI().getPath())));
        assertNotNull(factory.forName("hub"));
        assertNotNull(factory.forName("s3"));
        assertTrue(factory.forName("s3").isEnabled());
        assertTrue(factory.forType(Protocol.Type.s3).isEnabled());

        final DeviceSetupCallback proxy = new DeviceSetupCallback() {
            @Override
            public String displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) {
                return "firstLoginMockSetup";
            }

            @Override
            public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) {
                return new AccountKeyAndDeviceName().withAccountKey(setup.userConfig.setupCode).withDeviceName(String.format("firstLoginMockSetup %s", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                        .format(ZonedDateTime.now(ZoneId.of("Europe/Zurich")))));
            }

            @Override
            public String generateAccountKey() {
                return staticSetupCode();
            }
        };
        MockableDeviceSetupCallback.setProxy(proxy);

        final Host hub = new HostParser(factory).get(setup.hubURL).withCredentials(new Credentials(setup.userConfig.username, setup.userConfig.password));
        final HubSession session = (HubSession) SessionFactory.create(hub, new DefaultX509TrustManager(), new DefaultX509KeyManager())
                .withRegistry(VaultRegistryFactory.get(new DisabledPasswordCallback()));
        final LoginConnectionService login = new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
                PasswordStoreFactory.get(), new DisabledProgressListener());
        login.check(session, new DisabledCancelCallback());

        final BookmarkCollection bookmarks = BookmarkCollection.defaultCollection();
        bookmarks.add(hub);

        return session;
    }
}

