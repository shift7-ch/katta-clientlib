/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.DisabledProxyFinder;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.vault.DefaultVaultRegistry;
import ch.cyberduck.core.vault.LoadingVaultLookupListener;
import ch.cyberduck.core.vault.registry.VaultRegistryListService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.model.S3SERVERSIDEENCRYPTION;
import ch.iterate.hub.client.model.S3STORAGECLASSES;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.HubStorageProfileListService;
import ch.iterate.hub.protocols.hub.HubStorageProfileSyncSchedulerService;
import ch.iterate.hub.protocols.hub.HubStorageVaultSyncSchedulerService;
import ch.iterate.hub.protocols.s3.S3AssumeRoleSession;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.HubTestConfig;
import ch.iterate.hub.testsetup.MethodIgnorableSource;
import ch.iterate.hub.workflows.CreateVaultService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static ch.iterate.hub.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    // allow for 15s time difference
    private static final int LAG = 15000;

    /**
     * Verify storage profiles are synced from hub bookmark.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void test01Bootstrapping(final HubTestConfig hubTestConfig) throws Exception {
        log.info(String.format("M01 %s", hubTestConfig));

        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        try {

            final ApiClient adminApiClient = getAdminApiClient(hubTestConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);

            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.registerModule(new JsonNullableModule());
            try {
                adminStorageProfileApi.apiStorageprofileS3Put(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/aws_static/aws_static_profile.json"), StorageProfileS3Dto.class).storageClass(S3STORAGECLASSES.STANDARD));
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
                adminStorageProfileApi.apiStorageprofileS3stsPut(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/aws_sts/aws_sts_profile.json"), StorageProfileS3STSDto.class)
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
                adminStorageProfileApi.apiStorageprofileS3Put(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/minio_static/minio_static_profile.json"), StorageProfileS3Dto.class).storageClass(S3STORAGECLASSES.STANDARD));
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
                final StorageProfileS3STSDto storageProfile = mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/minio_sts/minio_sts_profile.json"), StorageProfileS3STSDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD).bucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
                adminStorageProfileApi.apiStorageprofileS3stsPut(storageProfile);
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }

            // trigger blocking sync
            new HubStorageProfileSyncSchedulerService(hubSession).operate(new DisabledLoginCallback());

            log.info(String.format("%s Protocols found:", ProtocolFactory.get().find().size()));
            for(Protocol protocol : ProtocolFactory.get().find()) {
                log.info(String.format("-  %s", protocol));
            }

            // aws static
            assertNotNull(ProtocolFactory.get().forName("72736C19-283C-49D3-80A5-AB74B5202543".toLowerCase()));
            // aws sts
            assertNotNull(ProtocolFactory.get().forName("844BD517-96D4-4787-BCFA-238E103149F6".toLowerCase()));
            // minio static
            assertNotNull(ProtocolFactory.get().forName("71B910E0-2ECC-46DE-A871-8DB28549677E".toLowerCase()));
            // minio sts
            assertNotNull(ProtocolFactory.get().forName("732D43FA-3716-46C4-B931-66EA5405EF1C".toLowerCase()));
        }
        catch(ApiException e) {
            log.error(String.format("%s %s", e.getCode(), e.getMessage()), e);
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
    public void test02AddStorageProfile(final HubTestConfig hubTestConfig) throws Exception {
        log.info(String.format("M02 %s", hubTestConfig));

        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        try {
            final ApiClient adminApiClient = getAdminApiClient(hubTestConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            final List<StorageProfileDto> storageProfiles = adminStorageProfileApi.apiStorageprofileGet(null);

            final UUID uuid = UUID.randomUUID();

            // trigger blocking sync
            new HubStorageProfileSyncSchedulerService(hubSession).operate(new DisabledLoginCallback());
            final int numProtocols = ProtocolFactory.get().find().size();

            log.info(String.format("Add storage profile for UUID %s", uuid));
            Assertions.assertNull(ProtocolFactory.get().forName(uuid.toString().toLowerCase()));

            final StorageProfileDto storageProfile = storageProfiles.get(0);
            // client-generated code is not subclassed...
            if(storageProfile.getActualInstance() instanceof StorageProfileS3STSDto) {
                final StorageProfileS3STSDto profile = (StorageProfileS3STSDto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3stsPut(profile);
            }
            else if(storageProfile.getActualInstance() instanceof StorageProfileS3Dto) {
                final StorageProfileS3Dto profile = (StorageProfileS3Dto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3Put(profile);
            }
            else {
                fail();
            }
            assertEquals(storageProfiles.size() + 1, adminStorageProfileApi.apiStorageprofileGet(null).size());

            new HubStorageProfileSyncSchedulerService(hubSession).operate(new DisabledLoginCallback());

            assertEquals(numProtocols + 1, ProtocolFactory.get().find().size());
            assertNotNull(ProtocolFactory.get().forName(uuid.toString().toLowerCase()));
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
    public void test03AddVault(final HubTestConfig config) throws Exception {
        log.info(String.format("M03 %s", config));

        final HubSession hubSession = setupConnection(config.setup);
        try {
            final ApiClient adminApiClient = getAdminApiClient(config.setup);
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
            log.info(String.format("Coercing storage profiles %s", storageProfiles));
            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(config.vault.storageProfileId.toLowerCase())).findFirst().get();
            assertNotNull(new HubStorageProfileListService(hubSession).list(Home.ROOT, new DisabledListProgressListener()).find(p -> StringUtils.equals(p.attributes().getFileId(),
                    config.vault.storageProfileId.toLowerCase())));

            log.info(String.format("Creating vault in %s", hubSession));
            final UUID vaultId = UUID.randomUUID();
            new CreateVaultService(hubSession).createVault(storageProfileWrapper, vaultId, new CreateVaultService.CreateVaultModel(
                    vaultId, "vault", null,
                    config.vault.storageProfileId, config.vault.username, config.vault.password, config.vault.bucketName, config.vault.region, true, 3));
            log.info(String.format("Getting vault bookmark for vault %s", vaultId));
            final Host vaultBookmark = new HubStorageVaultSyncSchedulerService(hubSession).toBookmark(vaultId, FirstLoginDeviceSetupCallback.disabled);
            log.info(String.format("Using vault bookmark %s", vaultBookmark));

            final Session<?> session = new S3AssumeRoleSession(vaultBookmark, new DisabledX509TrustManager(), new DefaultX509KeyManager());
            session.open(new DisabledProxyFinder(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
            session.login(new DisabledLoginCallback(), new DisabledCancelCallback());

            log.info(String.format("Listing bucket %s in %s", vaultBookmark.getDefaultPath(), vaultBookmark));
            final Date before = new Date();
            final Path bucket = new Path(vaultBookmark.getDefaultPath(), EnumSet.of(Path.Type.directory, Path.Type.volume));
            AttributedList<Path> bucketListRaw = session.getFeature(ListService.class).list(bucket, new DisabledListProgressListener());
            final Date after = new Date();

            log.info(before);
            log.info(after);
            log.info("paths:");

            for(final Path path : bucketListRaw) {
                Date date = new Date();
                date.setTime(path.attributes().getModificationDate());
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE dd-MMM-yy HH:mm:ssZ");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
                log.info(String.format("%s %s [%s,%s]", path,
                        simpleDateFormat.format(date),
                        simpleDateFormat.format(before),
                        simpleDateFormat.format(after))
                );

                if(path.isFile()) {
                    date.setTime(path.attributes().getModificationDate() + LAG);
                    assertTrue(date.after(before));
                    date.setTime(path.attributes().getModificationDate() - LAG);
                    assertTrue(date.before(after));
                }
            }
            final Path expectedPath = new Path(bucket, PreferencesFactory.get().getProperty("cryptomator.vault.config.filename"), EnumSet.of(AbstractPath.Type.file));
            log.info("expectedPath {}", expectedPath);
            assertNotNull(bucketListRaw.find(new SimplePathPredicate(expectedPath)));

            final ListService proxy = session.getFeature(ListService.class);
            final DefaultVaultRegistry registry = new DefaultVaultRegistry(new DisabledPasswordCallback());
            final ListService listService = new VaultRegistryListService(session, proxy, registry,
                    new LoadingVaultLookupListener(registry, new DisabledPasswordCallback()));

            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 should this only list the vault and not more?
//            final AttributedList<Path> bucketList = ff.list(Home.ROOT, new DisabledListProgressListener());
//            assertEquals(1, bucketList.size());
//            assertTrue(bucketList.contains(bucket));
            final AttributedList<Path> vaultContents = listService.list(bucket, new DisabledListProgressListener());
            for(final Path path : vaultContents) {
                log.info(path);
            }
            assertEquals(2, vaultContents.size());
            assertNotNull(vaultContents.find(new SimplePathPredicate(expectedPath)));
            assertNotNull(vaultContents.find(new SimplePathPredicate(new Path(bucket, "d", EnumSet.of(Path.Type.directory, AbstractPath.Type.placeholder)))));
        }
        finally {
            hubSession.close();
        }
    }
}
