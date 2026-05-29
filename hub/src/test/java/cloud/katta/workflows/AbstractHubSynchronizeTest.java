/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultProvider;
import ch.cyberduck.core.vault.VaultRegistry;
import ch.cyberduck.core.vault.VaultVersion;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.JSON;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.WithCounts;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubStorageLocationService;
import cloud.katta.protocols.hub.HubVaultRegistry;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestUtilities;
import cloud.katta.testsetup.MethodIgnorableSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    /**
     * Verify storage profiles are synced from hub bookmark.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test01Bootstrapping(final HubTestConfig config) throws Exception {
        log.info("M01 {}", config);
        final HubTestConfig.Setup.DockerConfig dockerConfig = config.setup.dockerConfig;
        final Properties configuration = new Properties();
        try (InputStream in = Objects.requireNonNull(this.getClass().getResourceAsStream(dockerConfig.envFile))) {
            configuration.load(in);
        }
        try (final HubSession adminHubSession = setupConnection(config.setup.hubURL, config.setup.adminConfig, config.vault); final HubSession hubSession = setupConnection(config.setup.hubURL, config.setup.userConfig, config.vault)) {
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminHubSession.getClient());

            final ObjectMapper mapper = new JSON().getMapper();
            try {
                adminStorageProfileApi.apiStorageprofilePost(new StorageProfileDto(mapper.readValue(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/setup/aws_static/storage_profile.json")), StorageProfileS3StaticDto.class)));
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }

            try {
                adminStorageProfileApi.apiStorageprofilePost(new StorageProfileDto(mapper.readValue(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/setup/aws_sts/storage_profile.json")), StorageProfileS3STSDto.class).storageClass(S3StorageClass.STANDARD)));
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }

            try (InputStream in = this.getClass().getResourceAsStream("/setup/minio_static/storage_profile.json")) {
                final String json = IOUtils.toString(Objects.requireNonNull(in), StandardCharsets.UTF_8)
                        .replace("${MINIO_SCHEME}", configuration.getProperty("MINIO_SCHEME"))
                        .replace("${MINIO_HOSTNAME}", configuration.getProperty("MINIO_HOSTNAME"))
                        .replace("${MINIO_PORT}", configuration.getProperty("MINIO_PORT"));
                adminStorageProfileApi.apiStorageprofilePost(new StorageProfileDto(mapper.readValue(json, StorageProfileS3StaticDto.class)));
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }

            try (InputStream in = this.getClass().getResourceAsStream("/setup/minio_sts/storage_profile.json")) {
                final String json = IOUtils.toString(Objects.requireNonNull(in), StandardCharsets.UTF_8)
                        .replace("${MINIO_SCHEME}", configuration.getProperty("MINIO_SCHEME"))
                        .replace("${MINIO_HOSTNAME}", configuration.getProperty("MINIO_HOSTNAME"))
                        .replace("${MINIO_PORT}", configuration.getProperty("MINIO_PORT"));
                adminStorageProfileApi.apiStorageprofilePost(new StorageProfileDto(mapper.readValue(json, StorageProfileS3STSDto.class)));
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }
            final List<StorageProfileDto> storageProfileDtos = new StorageProfileResourceApi(hubSession.getClient())
                    .apiStorageprofileGet(false);
            assertFalse(storageProfileDtos.isEmpty());

            // aws static
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId()
                    .equals(UUID.fromString("72736C19-283C-49D3-80A5-AB74B5202549"))));
            // aws sts
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId()
                    .equals(UUID.fromString("844BD517-96D4-4787-BCFA-238E103149F6"))));
            // minio static
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId()
                    .equals(UUID.fromString("71B910E0-2ECC-46DE-A871-8DB28549677E"))));
            // minio sts
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId()
                    .equals(UUID.fromString("732D43FA-3716-46C4-B931-66EA5405EF1C"))));
        }
        catch(ApiException e) {
            log.error("{} {}", e.getCode(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Verify sync after adding new storage profile.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test02AddStorageProfile(final HubTestConfig config) throws Exception {
        log.info("M02 {}", config);
        try (final HubSession adminHubSession = setupConnection(config.setup.hubURL, config.setup.adminConfig, config.vault)) {
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminHubSession.getClient());
            final List<StorageProfileDto> storageProfiles = adminStorageProfileApi.apiStorageprofileGet(null);

            final UUID uuid = UUID.randomUUID();

            final StorageProfileDto storageProfile = storageProfiles.get(0);
            // client-generated code is not subclassed...
            if(storageProfile.getActualInstance() instanceof StorageProfileS3STSDto) {
                final StorageProfileS3STSDto profile = (StorageProfileS3STSDto) storageProfile.getActualInstance();
                final Field f = profile.getClass().getField("id");
                f.set(f, uuid.toString());
                adminStorageProfileApi.apiStorageprofilePost(new StorageProfileDto(profile));
            }
            else if(storageProfile.getActualInstance() instanceof StorageProfileS3StaticDto) {
                final StorageProfileS3StaticDto profile = (StorageProfileS3StaticDto) storageProfile.getActualInstance();
                final Field f = profile.getClass().getField("id");
                f.set(f, uuid.toString());
                adminStorageProfileApi.apiStorageprofilePost(new StorageProfileDto(profile));
            }
            else {
                fail();
            }
            assertEquals(storageProfiles.size() + 1, adminStorageProfileApi.apiStorageprofileGet(null).size());
        }
    }

    /**
     * Create vaults for {MinIO S3,}x{STS,static} and list files encrypted and decrypted.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test03AddVault(final HubTestConfig config) throws Exception {
        log.info("M03 {}", config);

        try (final HubSession hubSession = setupConnection(config.setup.hubURL, config.setup.userConfig, config.vault);
             final HubSession adminHubSession = setupConnection(config.setup.hubURL, config.setup.adminConfig, config.vault)) {
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminHubSession.getClient()).apiStorageprofileGet(false);
            log.info("Coercing storage profiles {}", storageProfiles);
            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(config.vault.storageProfileId.toLowerCase())).findFirst().get();

            log.info("Creating vault in {}", hubSession);

            final Path vaultName = new Path(String.format("Vault %s", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.volume, Path.Type.directory));
            final HubStorageLocationService.StorageLocation location = new HubStorageLocationService.StorageLocation(storageProfileWrapper.getId().toString(), storageProfileWrapper.getRegion(),
                    storageProfileWrapper.getName());

            final VaultProvider vaultProvider = hubSession.getFeature(VaultProvider.class);
            final Vault cryptomator = vaultProvider.create(hubSession, location.getIdentifier(), vaultName, new VaultVersion(VaultVersion.Type.UVF),
                    new VaultCredentials());

            final AttributedList<Path> vaults = hubSession.getFeature(ListService.class).list(Home.root(), new DisabledListProgressListener());
            assertFalse(vaults.isEmpty());

            final VaultRegistry vaultRegistry = hubSession.getRegistry();
            assertInstanceOf(HubVaultRegistry.class, vaultRegistry);
            assertNotSame(Vault.DISABLED, vaultRegistry.find(hubSession, cryptomator.getHome()));
            assertTrue(vaultRegistry.contains(cryptomator.getHome()));
            final Path vault = vaults.find(new SimplePathPredicate(cryptomator.getHome()));
            assertNotNull(vault);
            assertTrue(vault.getType().contains(Path.Type.vault));
            {
                assertTrue(hubSession.getFeature(Find.class).find(vault));
                assertEquals(location.getRegion(), hubSession.getFeature(AttributesFinder.class).find(vault).getRegion());
                assertThrows(UnsupportedException.class, () -> hubSession.getFeature(Move.class).preflight(vault, Optional.empty()));
                assertThrows(UnsupportedException.class, () -> hubSession.getFeature(Delete.class).preflight(vault));
                // decrypted file listing
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertTrue(list.isEmpty());
            }
            {
                // encrypted file upload
                final Path file = new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(234));
                assertTrue(hubSession.getFeature(Find.class).find(file));
                assertEquals(content.length, hubSession.getFeature(AttributesFinder.class).find(file).getSize());
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(1, list.size());
                assertEquals(file.getName(), list.get(0).getName());
                assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
                // Assert preflight checks
                hubSession.getFeature(Read.class).preflight(file);
                hubSession.getFeature(Write.class).preflight(file);
                hubSession.getFeature(Delete.class).preflight(file);
                hubSession.getFeature(Move.class).preflight(file, Optional.empty());
            }
            {
                // directory creation and listing
                final Path folder = new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
                hubSession.getFeature(Directory.class).mkdir(hubSession.getFeature(Write.class), folder, new TransferStatus());
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(2, list.size()); // a file and a folder
                {
                    // file upload in subfolder
                    final Path file = new Path(folder, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                    final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(555));
                    final AttributedList<Path> sublist = hubSession.getFeature(ListService.class).list(folder, new DisabledListProgressListener());
                    assertEquals(1, sublist.size());
                    assertEquals(file.getName(), sublist.get(0).getName());
                    assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
                    // move operation to root folder and read again
                    hubSession.getFeature(Move.class).move(file, new Path(vault, file.getName(), EnumSet.of(Path.Type.file)), new TransferStatus(),
                            new Delete.DisabledCallback(), new DisabledConnectionCallback());
                    final AttributedList<Path> list2 = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                    assertEquals(3, list2.size()); // 1 subfolder and 2 files
                    assertEquals(1, list2.filter(Path::isDirectory).size());
                    assertEquals(2, list2.filter(Path::isFile).size());
                }
            }
            vaultRegistry.close(vault);
            assertFalse(vaultRegistry.contains(vault));
            assertSame(Vault.DISABLED, vaultRegistry.find(hubSession, vault));
            assertThrows(AccessDeniedException.class, () -> hubSession.getFeature(ListService.class).preflight(vault));
            assertThrows(NotfoundException.class, () -> hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener()));
        }
    }

    /**
     * List all vaults for {MinIO S3,}x{STS,static}, and create directories and files in them and delete them again
     */
    @ParameterizedTest
    @MethodSource("arguments")
    void test04CreateReadDeleteFilesAndDirectoriesInAllVaults(final HubTestConfig config) throws Exception {
        try (final HubSession hubSession = setupConnection(config.setup.hubURL, config.setup.userConfig, config.vault)) {
            final ListService feature = hubSession.getFeature(ListService.class);
            final AttributedList<Path> vaults = feature.list(Home.root(), new DisabledListProgressListener());
            for(final Path vault : vaults) {
                assertTrue(hubSession.getFeature(Find.class).find(vault));
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(3, list.size()); // 1 subfolder and 2 files
                assertEquals(1, list.toStream().filter(Path::isDirectory).count());
                assertEquals(2, list.toStream().filter(Path::isFile).count());
                for(final Path f : list.filter(Path::isFile)) {
                    final long length = f.attributes().getSize();
                    HubTestUtilities.read(hubSession, f, (int) length);
                }
                for(final Path d : list.filter(Path::isDirectory)) {
                    {
                        // New file: create, read and delete
                        final Path file = new Path(d, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                        final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(247));
                        assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
                        hubSession.getFeature(Delete.class).delete(Collections.singletonList(file), PasswordCallback.noop, new Delete.DisabledCallback());
                        assertFalse(hubSession.getFeature(Find.class).find(file));
                    }
                    {
                        // New directory: create and delete
                        final Path folder = new Path(d, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
                        hubSession.getFeature(Directory.class).mkdir(hubSession.getFeature(Write.class), folder, new TransferStatus());
                        hubSession.getFeature(Delete.class).delete(Collections.singletonList(folder), PasswordCallback.noop, new Delete.DisabledCallback());
                        assertFalse(hubSession.getFeature(Find.class).find(folder));
                    }
                    final AttributedList<Path> sublist = hubSession.getFeature(ListService.class).list(d, new DisabledListProgressListener());
                    for(Path f : sublist.filter(Path::isFile)) {
                        final long length = f.attributes().getSize();
                        HubTestUtilities.read(hubSession, f, (int) length);
                    }
                }
            }
        }
    }


    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test05AddVaultAndShareWithAlice(final HubTestConfig config) throws Exception {
        log.info("M05 {}", config);
        try (final HubSession adminHubSession = setupConnection(config.setup.hubURL, config.setup.adminConfig, config.vault); final HubSession aliceHubSession = setupConnection(config.setup.hubURL, config.setup.userConfig, config.vault)) {
            final WithCounts alice = new UsersResourceApi(adminHubSession.getClient()).apiUsersGet().stream().filter(wc -> wc.getName().equals(config.setup.userConfig.username)).findFirst().get();
            final UserDto admin = new UsersResourceApi(adminHubSession.getClient()).apiUsersMeGet(false, false);
            final UUID vaultId;
            final String name;
            {
                // admin creates vault
                final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminHubSession.getClient()).apiStorageprofileGet(false);
                log.info("Coercing storage profiles {}", storageProfiles);
                final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                        .map(StorageProfileDtoWrapper::coerce)
                        .filter(p -> p.getId().toString().equals(config.vault.storageProfileId.toLowerCase())).findFirst().get();

                log.info("Creating vault in {}", adminHubSession);

                final Path vaultName = new Path(String.format("Vault %s", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.volume, Path.Type.directory));
                final HubStorageLocationService.StorageLocation location = new HubStorageLocationService.StorageLocation(storageProfileWrapper.getId().toString(), storageProfileWrapper.getRegion(),
                        storageProfileWrapper.getName());
                final VaultProvider vaultProvider = adminHubSession.getFeature(VaultProvider.class);
                final Vault vault = vaultProvider.create(adminHubSession, location.getIdentifier(), vaultName, new VaultVersion(VaultVersion.Type.UVF),
                        new VaultCredentials());
                vaultId = UUID.fromString(StringUtils.removeStart(vault.getHome().getName(), storageProfileWrapper.getBucketPrefix()));
                name = vault.getHome().getName();

                // admin share new vault with alice without granting access
                final Map<String, Role> members = new java.util.HashMap<>();
                members.put(alice.getId(), Role.MEMBER);
                members.put(admin.getId(), Role.OWNER);
                new VaultResourceApi(adminHubSession.getClient()).apiVaultsVaultIdMembersPut(vaultId, members);
            }
            {
                // vault not listed for alice
                final ListService feature = aliceHubSession.getFeature(ListService.class);
                final AttributedList<Path> vaults = feature.list(Home.root(), new DisabledListProgressListener());
                assertFalse(vaults.toStream().anyMatch(path -> path.getName().equals(name)));
            }
            {
                // before granting access
                assertEquals(1, new VaultResourceApi(adminHubSession.getClient()).apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId).size());
                assertEquals(alice.getId(), new VaultResourceApi(adminHubSession.getClient()).apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId).get(0).getId());

                // admin grants access to alice
                final UserKeys userKeys = new UserKeysServiceImpl(adminHubSession).getUserKeys(adminHubSession.getHost(), adminHubSession.getMe(),
                        new DeviceKeysServiceImpl().getDeviceKeys(adminHubSession.getHost(), adminHubSession.getMe()));
                new GrantAccessServiceImpl(
                        new VaultResourceApi(adminHubSession.getClient()),
                        new UsersResourceApi(adminHubSession.getClient())).grantAccessToUsersRequiringAccessGrant(vaultId, userKeys);
                assertEquals(new VaultResourceApi(adminHubSession.getClient()).apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId).size(), 0);
            }
            {
                // vault listed for alice
                final ListService feature = aliceHubSession.getFeature(ListService.class);
                final AttributedList<Path> vaults = feature.list(Home.root(), new DisabledListProgressListener());
                assertTrue(vaults.toStream().anyMatch(path -> path.getName().equals(name)));
            }
        }
    }
}
