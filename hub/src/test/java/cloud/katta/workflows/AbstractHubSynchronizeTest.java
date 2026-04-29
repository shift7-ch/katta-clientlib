/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
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
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultProvider;
import ch.cyberduck.core.vault.VaultRegistry;
import ch.cyberduck.core.vault.VaultVersion;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.JSON;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubStorageLocationService;
import cloud.katta.protocols.hub.HubVaultRegistry;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestUtilities;
import cloud.katta.testsetup.MethodIgnorableSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    /**
     * Verify storage profiles are synced from hub bookmark.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test01Bootstrapping(final HubTestConfig testConfig) throws Exception {
        log.info("M01 {}", testConfig);
        final HubTestConfig.Setup.DockerConfig dockerConfig = testConfig.setup.dockerConfig;
        final Properties configuration = new Properties();
        try (InputStream in = Objects.requireNonNull(this.getClass().getResourceAsStream(dockerConfig.envFile))) {
            configuration.load(in);
        }
        final HubSession hubSession = setupConnection(testConfig);
        try {

            final ApiClient adminApiClient = getAdminApiClient(testConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            assertTrue(new StorageProfileResourceApi(hubSession.getClient()).apiStorageprofileGet(true).isEmpty());

            final ObjectMapper mapper = new JSON().getMapper();
            try {
                adminStorageProfileApi.apiStorageprofileS3staticPost(Objects.requireNonNull(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream(
                                String.format("/setup/%s/aws_static/storage_profile.json",
                                        dockerConfig.profile)), StorageProfileS3StaticDto.class))
                        .storageClass(S3STORAGECLASSES.STANDARD)
                );
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
                adminStorageProfileApi.apiStorageprofileS3stsPost(Objects.requireNonNull(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream(
                                String.format("/setup/%s/aws_sts/storage_profile.json",
                                        dockerConfig.profile)), StorageProfileS3STSDto.class))
                        .storageClass(S3STORAGECLASSES.STANDARD).bucketEncryption(S3SERVERSIDEENCRYPTION.NONE));
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
                final String json = IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(
                                String.format("/setup/%s/minio_static/storage_profile.json", dockerConfig.profile))), StandardCharsets.UTF_8)
                        .replace("MINIO_HOSTNAME", configuration.getProperty("MINIO_HOSTNAME"))
                        .replace("MINIO_PORT", configuration.getProperty("MINIO_PORT"));
                final StorageProfileS3StaticDto storageProfile = mapper.readValue(json, StorageProfileS3StaticDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD);
                adminStorageProfileApi.apiStorageprofileS3staticPost(storageProfile);
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
                final String json = IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(
                                String.format("/setup/%s/minio_sts/storage_profile.json", dockerConfig.profile))), StandardCharsets.UTF_8)
                        .replace("MINIO_HOSTNAME", configuration.getProperty("MINIO_HOSTNAME"))
                        .replace("MINIO_PORT", configuration.getProperty("MINIO_PORT"));
                final StorageProfileS3STSDto storageProfile = mapper.readValue(json, StorageProfileS3STSDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD)
                        .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
                adminStorageProfileApi.apiStorageprofileS3stsPost(storageProfile);
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
        finally {
            hubSession.close();
        }
    }

    /**
     * Verify sync after adding new storage profile.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test02AddStorageProfile(final HubTestConfig hubTestConfig) throws Exception {
        log.info("M02 {}", hubTestConfig);

        final HubSession hubSession = setupConnection(hubTestConfig);
        try {
            final ApiClient adminApiClient = getAdminApiClient(hubTestConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            final List<StorageProfileDto> storageProfiles = adminStorageProfileApi.apiStorageprofileGet(null);

            final UUID uuid = UUID.randomUUID();

            final StorageProfileDto storageProfile = storageProfiles.get(0);
            // client-generated code is not subclassed...
            if(storageProfile.getActualInstance() instanceof StorageProfileS3STSDto) {
                final StorageProfileS3STSDto profile = (StorageProfileS3STSDto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3stsPost(profile);
            }
            else if(storageProfile.getActualInstance() instanceof StorageProfileS3StaticDto) {
                final StorageProfileS3StaticDto profile = (StorageProfileS3StaticDto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3staticPost(profile);
            }
            else {
                fail();
            }
            assertEquals(storageProfiles.size() + 1, adminStorageProfileApi.apiStorageprofileGet(null).size());
        }
        finally {
            hubSession.close();
        }
    }

    /**
     * Create vaults for {MinIO S3,}x{STS,static} and list files encrypted and decrypted.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test03AddVault(final HubTestConfig config) throws Exception {
        log.info("M03 {}", config);

        final HubSession hubSession = setupConnection(config);
        try {
            final ApiClient adminApiClient = getAdminApiClient(config.setup);
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
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
            {
                assertNotNull(vaults.find(new SimplePathPredicate(cryptomator.getHome())));
                assertTrue(hubSession.getFeature(Find.class).find(cryptomator.getHome()));
                assertEquals(location.getRegion(), hubSession.getFeature(AttributesFinder.class).find(cryptomator.getHome()).getRegion());

                assertNotSame(Vault.DISABLED, vaultRegistry.find(hubSession, cryptomator.getHome()));
                assertTrue(vaultRegistry.contains(cryptomator.getHome()));
            }

            final Path vault = vaults.find(new SimplePathPredicate(cryptomator.getHome()));
            {
                // decrypted file listing
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertTrue(list.isEmpty());
            }
            {
                // encrypted file upload
                final Path file = new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(234));
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(1, list.size());
                assertEquals(file.getName(), list.get(0).getName());
                assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
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
        finally {
            hubSession.close();
        }
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void test04SetupCode(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config);
        final ListService feature = hubSession.getFeature(ListService.class);
        final AttributedList<Path> vaults = feature.list(Home.root(), new DisabledListProgressListener());
        assertEquals(vaults, feature.list(Home.root(), new DisabledListProgressListener()));
        for(final Path vault : vaults) {
            assertTrue(hubSession.getFeature(Find.class).find(vault));
            final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
            assertEquals(3, list.size()); // 1 subfolder and 2 files
            assertEquals(1, list.toStream().filter(Path::isDirectory).count());
            assertEquals(2, list.toStream().filter(Path::isFile).count());
            for(Path f : list.filter(Path::isFile)) {
                final long length = f.attributes().getSize();
                HubTestUtilities.read(hubSession, f, (int) length);
            }
            for(Path d : list.filter(Path::isDirectory)) {
                {
                    // New file
                    final Path file = new Path(d, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                    final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(247));
                    assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
                    hubSession.getFeature(Delete.class).delete(Collections.singletonList(file), PasswordCallback.noop, new Delete.DisabledCallback());
                    assertFalse(hubSession.getFeature(Find.class).find(file));
                }
                {
                    // New directory
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
