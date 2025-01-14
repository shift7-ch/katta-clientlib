/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.vault.DefaultVaultRegistry;
import ch.cyberduck.core.vault.LoadingVaultLookupListener;
import ch.cyberduck.core.vault.registry.VaultRegistryListService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.model.S3SERVERSIDEENCRYPTION;
import ch.iterate.hub.client.model.S3STORAGECLASSES;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;
import ch.iterate.hub.core.callback.CreateVaultModel;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.model.StorageProfileDtoWrapperException;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.VaultProfileBookmarkService;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.HubTestController;
import ch.iterate.hub.testsetup.MethodIgnorableSource;
import ch.iterate.hub.testsetup.model.HubTestConfig;
import ch.iterate.hub.testsetup.model.HubTestSetupConfig;
import ch.iterate.hub.testsetup.model.VaultSpec;
import ch.iterate.hub.workflows.CreateVaultService;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static ch.iterate.hub.testsetup.HubTestUtilities.*;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    // allow for 15s time difference
    private static int LAG = 15000;

    /**
     * Verify storage profiles are synced from hub bookmark.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void test01Bootstrapping(final HubTestConfig hubTestConfig) throws Exception {
        log.info(String.format("M01 %s", hubTestConfig));
        final HubSession hubSession = setupForUser(hubTestConfig.hubTestSetupConfig, hubTestConfig.hubTestSetupConfig.USER_001());
        try {
            final HubTestSetupConfig hubTestSetupConfig = hubTestConfig.hubTestSetupConfig;

            assertNotNull(ProtocolFactory.get().forName("s3-hub", "Shift 7 GmbH"));
            assertNotNull(ProtocolFactory.get().forName("s3-hub-sts", "Shift 7 GmbH"));

            new CreateHubBookmarkAction(hubTestSetupConfig.hubURL(), BookmarkCollection.defaultCollection(), new HubTestController()).run();

            final ApiClient adminApiClient = getAdminApiClient(hubTestSetupConfig);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            assertNotNull(ProtocolFactory.get().forName(hubSession.getConfigApi().apiConfigGet().getUuid().toString()));

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

            // wait for sync
            Thread.sleep(SYNC_WAIT_SECS * 1000);

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
    }

    /**
     * Verify sync after adding new storage profile.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void test02AddStorageProfile(final HubTestConfig hubTestConfig) throws Exception {
        log.info(String.format("M02 %s", hubTestConfig));

        final HubTestSetupConfig hubTestSetupConfig = hubTestConfig.hubTestSetupConfig;
        setupForUser(hubTestSetupConfig, hubTestConfig.hubTestSetupConfig.USER_001());

        new CreateHubBookmarkAction(hubTestSetupConfig.hubURL(), BookmarkCollection.defaultCollection(), new HubTestController()).run();

        final ApiClient adminApiClient = getAdminApiClient(hubTestSetupConfig);
        final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
        final List<StorageProfileDto> storageProfiles = adminStorageProfileApi.apiStorageprofileGet(null);

        final UUID uuid = UUID.randomUUID();

        // wait for first time sync for admin before we count protocols
        Thread.sleep(SYNC_WAIT_SECS * 1000);
        final int numProtocols = ProtocolFactory.get().find().size();

        log.info(String.format("Add storage profile for UUID %s", uuid));
        assertNull(ProtocolFactory.get().forName(uuid.toString().toLowerCase()));

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
            Assert.fail();
        }
        assertEquals(storageProfiles.size() + 1, adminStorageProfileApi.apiStorageprofileGet(null).size());

        // wait for next sync
        Thread.sleep(SYNC_WAIT_SECS * 1000);
        assertEquals(numProtocols + 1, ProtocolFactory.get().find().size());
        assertNotNull(ProtocolFactory.get().forName(uuid.toString().toLowerCase()));
    }

    /**
     * Create vaults for {MinIO S3,}x{STS,static} and list files encrypted and decrypted.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void test03AddVault(final HubTestConfig hubTestConfig) throws Exception {
        log.info(String.format("M03 %s", hubTestConfig));

        final HubTestSetupConfig hubTestSetupConfig = hubTestConfig.hubTestSetupConfig;
        final HubSession hubSession = setupForUser(hubTestSetupConfig, hubTestConfig.hubTestSetupConfig.USER_001());
        final VaultSpec vaultSpec = hubTestConfig.vaultSpec;


        final String storageProfileId = vaultSpec.storageProfileId;
        final String bucketName = vaultSpec.bucketName;
        final String username = vaultSpec.username;
        final String password = vaultSpec.password;

        final ApiClient adminApiClient = getAdminApiClient(hubTestSetupConfig);
        final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
        log.info(String.format("Coercing storage profiles %s", storageProfiles));
        final StorageProfileDtoWrapper storageProfile = storageProfiles.stream()
                .map(StorageProfileDtoWrapper::coerce)
                .filter(p -> {
                    try {
                        return p.getId().toString().equals(storageProfileId.toLowerCase());
                    }
                    catch(StorageProfileDtoWrapperException e) {
                        log.error(e);
                        return false;
                    }
                }).findFirst().get();

        if(bucketName != null) {
            log.info(String.format("Empty bucket %s", bucketName));

            final AmazonS3 s3Client = createS3ClientForEmptyingBucket(
                    bucketName,
                    storageProfile.getHostname() == null ? null : new HostUrlProvider().get(storageProfile.getScheme() == null ? Scheme.https : Scheme.valueOf(storageProfile.getScheme()), storageProfile.getPort() == null ? 443 : storageProfile.getPort().intValue(), null, storageProfile.getHostname(), bucketName), username, password, null, storageProfile.getRegion(), true);
            final List<DeleteObjectsRequest.KeyVersion> keys = s3Client.listObjectsV2(bucketName).getObjectSummaries().stream().map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey())).collect(Collectors.toList());
            if(!keys.isEmpty()) {
                DeleteObjectsRequest dor = new DeleteObjectsRequest(bucketName).withKeys(keys);
                s3Client.deleteObjects(dor);
            }
        }
        log.info(String.format("Creating vault in %s", hubSession));
        final UUID vaultUuid = UUID.randomUUID();
        new CreateVaultService(hubSession, new HubTestController()).createVault(new CreateVaultModel(vaultUuid, "no reason", String.format("my first vault %s", storageProfile.getName()), "", storageProfileId.toLowerCase(), username, password, bucketName, storageProfile.getRegion(), true, 3));
        log.info(String.format("Getting vault bookmark for vault %s", vaultUuid));
        final Host vaultBookmark = new VaultProfileBookmarkService(hubSession).getVaultBookmark(vaultUuid, new DisabledFirstLoginDeviceSetupCallback());
        log.info(String.format("Logging into vault %s with shared oauth credentials from password store", vaultBookmark));
        final Session<?> session = vaultLoginWithSharedOAuthCredentialsFromPasswordStore(vaultBookmark);

        log.info(String.format("Listing bucket %s in %s", vaultBookmark.getDefaultPath(), vaultBookmark));
        final Date before = new Date();
        AttributedList<Path> bucketListRaw = session.getFeature(ListService.class).list(new Path(String.format("/%s", vaultBookmark.getDefaultPath()), EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
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
                assert date.after(before);
                date.setTime(path.attributes().getModificationDate() - LAG);
                assert date.before(after);
            }
        }
        final Path expectedPath = new Path(String.format("/%s/%s", vaultBookmark.getDefaultPath(), PreferencesFactory.get().getProperty("cryptomator.vault.config.filename")), EnumSet.of(AbstractPath.Type.file));
        log.info("expectedPath {}", expectedPath);
        assert bucketListRaw.contains(expectedPath);

        final ListService proxy = session.getFeature(ListService.class);
        final DefaultVaultRegistry registry = new DefaultVaultRegistry(new DisabledPasswordCallback());
        final ListService ff = new VaultRegistryListService(session, proxy, registry,
                new LoadingVaultLookupListener(registry, new DisabledPasswordCallback()));

        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 should this only list the vault and not more?
//        final AttributedList<Path> bucketList = ff.list(new Path("/", EnumSet.of(Path.Type.directory, Path.Type.volume)), new DisabledListProgressListener());
//        for(final Path path : bucketList) {
//            log.info(path);
//        }
//        assertEquals(1, bucketList.size());
//        assertTrue(bucketList.contains(new Path(String.format("/%s", vaultBookmark.getDefaultPath()), EnumSet.of(Path.Type.directory, AbstractPath.Type.volume))));
        final AttributedList<Path> vaultContents = ff.list(new Path(String.format("/%s", vaultBookmark.getDefaultPath()), EnumSet.of(Path.Type.directory)), new DisabledListProgressListener());
        for(final Path path : vaultContents) {
            log.info(path);
        }
        assertEquals(2, vaultContents.size());
        assertTrue(vaultContents.contains(expectedPath));
        assertTrue(vaultContents.contains(new Path(String.format("/%s/d", vaultBookmark.getDefaultPath()), EnumSet.of(Path.Type.directory, AbstractPath.Type.placeholder))));
    }


    private static AmazonS3 createS3ClientForEmptyingBucket(final String bucketName, final String s3Endpoint, final String awsAccessKey, final String awsSecretKey, final String sessionToken, String region, final boolean pathStyleAccessEnabled) {
        AmazonS3ClientBuilder s3Builder = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(awsAccessKey, awsSecretKey, sessionToken)));
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicSessionCredentials(awsAccessKey, awsSecretKey, sessionToken)))
                .withRegion(Regions.DEFAULT_REGION)
                .enableUseArnRegion()
                .build();
        if(region == null) {
            try {
                HeadBucketResult headBucketResult = s3Client.headBucket(new HeadBucketRequest(bucketName));
                return s3Builder.withRegion(headBucketResult.getBucketRegion()).build();
            }
            catch(AmazonServiceException e) {
                if(e.getStatusCode() == 301) {
                    region = e.getHttpHeaders().getOrDefault("x-amz-bucket-region", null);
                    log.debug(String.format("Extracted x-amz-bucket-region %s from 301 bucket head", region));
                }
            }
        }
        if(s3Endpoint != null) {
            // region may be null
            log.debug(String.format("s3 client with endpoint configuration %s %s", s3Endpoint, region));
            s3Builder = s3Builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region));
        }
        else {
            log.debug(String.format("s3 client with region %s", region));
            s3Builder = s3Builder.withRegion(region);
        }
        log.debug(String.format("s3 client with pathStyleAccessEnabled=%s", pathStyleAccessEnabled));
        return s3Builder.withPathStyleAccessEnabled(pathStyleAccessEnabled).build();
    }
}
