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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Function;

import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.core.util.MockableDeviceSetupCallback;
import cloud.katta.model.AccountKeyAndDeviceName;
import cloud.katta.protocols.hub.HubProtocol;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubUVFVault;
import cloud.katta.protocols.hub.HubVaultRegistry;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@HubIntegrationTest
public abstract class AbstractHubTest extends VaultTest {

    static {
        // VaultTest is Junit 4 with @BeforeClass annotation, call statically in Jupiter setup.
        credentials();
    }

    private static final HubTestConfig.VaultSpec minioSTSVaultConfig = new HubTestConfig.VaultSpec("MinIO STS", "732D43FA-3716-46C4-B931-66EA5405EF1C",
            null, null, null, "eu-west-1");
    private static final HubTestConfig.VaultSpec minioStaticVaultConfig = new HubTestConfig.VaultSpec("MinIO static", "71B910E0-2ECC-46DE-A871-8DB28549677E",
            "handmade", "minioadmin", "minioadmin", null);
    private static final HubTestConfig.VaultSpec awsSTSVaultConfig = new HubTestConfig.VaultSpec("AWS STS", "844BD517-96D4-4787-BCFA-238E103149F6",
            null, null, null, "eu-west-1");
    private static final HubTestConfig.VaultSpec awsStaticVaultConfig = new HubTestConfig.VaultSpec("AWS static", "72736C19-283C-49D3-80A5-AB74B5202543",
            "handmade2",
            PROPERTIES.get("handmade2.s3.amazonaws.com.username"),
            PROPERTIES.get("handmade2.s3.amazonaws.com.password"),
            null
    );

    /**
     * Local: hub, Keycloak, MinIO started via testcontainers+docker-compose.
     */
    public static final HubTestConfig.Setup LOCAL;
    public static final HubTestConfig.Setup.DockerConfig LOCAL_DOCKER_CONFIG;

    static {
        LOCAL_DOCKER_CONFIG = new HubTestConfig.Setup.DockerConfig("/docker-compose-minio-localhost-hub.yml", "/.local.env", "local", "admin", "admin", "top-secret");
        LOCAL = new HubTestConfig.Setup()
                .withHubURL("http://localhost:8280")
                .withUserConfig(new HubTestConfig.Setup.UserConfig(new Credentials("alice", "asd"), staticSetupCode()))
                .withAdminConfig(new HubTestConfig.Setup.UserConfig(new Credentials("admin", "admin"), staticSetupCode()))
                .withDockerConfig(LOCAL_DOCKER_CONFIG);
    }

    private static final Function<HubTestConfig.VaultSpec, Arguments> argumentUnattendedLocalOnly = vs -> Arguments.of(Named.of(
            String.format("%s %s (Bucket %s)", vs.storageProfileName, LOCAL.hubURL, vs.bucketName),
            new HubTestConfig(LOCAL, vs)));


    public static final Arguments LOCAL_MINIO_STATIC = argumentUnattendedLocalOnly.apply(minioStaticVaultConfig);
    public static final Arguments LOCAL_MINIO_STS = argumentUnattendedLocalOnly.apply(minioSTSVaultConfig);

    /**
     * Local attended: re-use running local stetup.
     */
    private static final HubTestConfig.Setup LOCAL_ATTENDED = new HubTestConfig.Setup()
            .withHubURL("http://localhost:8080")
            .withAdminConfig(new HubTestConfig.Setup.UserConfig(new Credentials("admin", "admin"), staticSetupCode()))
            .withUserConfig(new HubTestConfig.Setup.UserConfig(new Credentials("alice", "asd"), staticSetupCode()));
    private static final Function<HubTestConfig.VaultSpec, Arguments> argumentAttendedLocalOnly = vs -> Arguments.of(Named.of(
            String.format("%s %s (Bucket %s)", vs.storageProfileName, LOCAL_ATTENDED.hubURL, vs.bucketName),
            new HubTestConfig(LOCAL_ATTENDED, vs)));

    /**
     * Hybrid: local hub (testcontainers+docker-compose) against AWS/MinIO/Keycloak remote.
     */
    public static final HubTestConfig.Setup HYBRID;
    public static final HubTestConfig.Setup.DockerConfig HYBRID_DOCKER_CONFIG;

    static {
        HYBRID_DOCKER_CONFIG = new HubTestConfig.Setup.DockerConfig(
                "/docker-compose-minio-localhost-hub.yml",
                "/.hybrid.env",
                null,
                PROPERTIES.get("testing.katta.cloud.chipotle.admin.name"),
                PROPERTIES.get("testing.katta.cloud.chipotle.admin.password"),
                PROPERTIES.get("testing.katta.cloud.chipotle.syncer.password")
        );
        HYBRID = new HubTestConfig.Setup()
                .withHubURL("http://localhost:8280")
                .withUserConfig(
                        new HubTestConfig.Setup.UserConfig(new Credentials(
                                PROPERTIES.get("testing.katta.cloud.chipotle.user.name"),
                                PROPERTIES.get("testing.katta.cloud.chipotle.user.password")),
                                staticSetupCode())
                )
                .withAdminConfig(
                        new HubTestConfig.Setup.UserConfig(new Credentials(
                                PROPERTIES.get("testing.katta.cloud.chipotle.admin.name"),
                                PROPERTIES.get("testing.katta.cloud.chipotle.admin.password")),
                                staticSetupCode())
                )
                .withDockerConfig(HYBRID_DOCKER_CONFIG);
    }

    private static final Function<HubTestConfig.VaultSpec, Arguments> argumentUnattendedHybrid = vs -> Arguments.of(Named.of(
            String.format("%s %s (Bucket %s)", vs.storageProfileName, HYBRID.hubURL, vs.bucketName),
            new HubTestConfig(HYBRID, vs)));


    public static final Arguments HYBRID_MINIO_STATIC = argumentUnattendedHybrid.apply(minioStaticVaultConfig);
    public static final Arguments HYBRID_MINIO_STS = argumentUnattendedHybrid.apply(minioSTSVaultConfig);
    public static final Arguments HYBRID_AWS_STATIC = argumentUnattendedHybrid.apply(awsStaticVaultConfig);
    public static final Arguments HYBRID_AWS_STS = argumentUnattendedHybrid.apply(awsSTSVaultConfig);

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
        preferences.setProperty("cloud.katta.min_api_level", 4);

        preferences.setProperty("s3.assumerole.tag", "VaultRequested");

        preferences.setProperty("tmp.dir", Files.createTempDirectory("cipherduck_test_setup_alice").toString());
    }

    private static String staticSetupCode() {
        // set to some value to have staticSetupCode instead of random string in attended local test setup.
        return "setupcode";
    }

    protected static HubSession setupConnection(final HubTestConfig.Setup setup) throws Exception {
        final ProtocolFactory factory = ProtocolFactory.get();
        // ProtocolFactory.get() is static, the profiles contains OAuth token URL, leads to invalid grant exceptions when this changes during class loading lifetime (e.g. if the same storage profile ID is deployed to the LOCAL and the HYBRID hub).
        for(final Protocol protocol : ProtocolFactory.get().find()) {
            if(protocol instanceof Profile) {
                factory.unregister((Profile) protocol);
            }
        }
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

        final DeviceSetupCallback proxy = deviceSetupCallback(setup);
        MockableDeviceSetupCallback.setProxy(proxy);

        final Scheme scheme = setup.hubURL.startsWith("https://") ? Scheme.https : Scheme.http;
        final Protocol profile = factory.find(p -> p.getScheme().equals(scheme) && p.getIdentifier().equals("hub") && p.isEnabled()).getFirst();
        factory.find(p -> p.getIdentifier().equals("hub") && !p.getScheme().equals(scheme) && p instanceof Profile).stream().forEach(p -> factory.unregister((Profile) p));
        assertNotNull(factory.forName("hub"));
        assertNotNull(factory.forName("s3"));
        final Host hub = new Host(profile).withCredentials(new Credentials(setup.userConfig.credentials).withOauth(new OAuthTokens(setup.userConfig.credentials.getOauth() != null ? setup.userConfig.credentials.getOauth() : OAuthTokens.EMPTY)));
        hub.setHostname(HostParser.parse(setup.hubURL).getHostname());
        hub.setDefaultPath(HostParser.parse(setup.hubURL).getDefaultPath());
        hub.setPort(URI.create(setup.hubURL).getPort() > 0 ? URI.create(setup.hubURL).getPort() : scheme.getPort());

        final HubSession session = (HubSession) SessionFactory.create(hub, new DefaultX509TrustManager(), new DefaultX509KeyManager())
                .withRegistry(VaultRegistryFactory.get(new DisabledPasswordCallback()));
        final LoginConnectionService login = new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
                PasswordStoreFactory.get(), new DisabledProgressListener());
        login.check(session, new DisabledCancelCallback());
        return session;
    }

    protected static @NotNull DeviceSetupCallback deviceSetupCallback(HubTestConfig.Setup setup) {
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
        return proxy;
    }
}

