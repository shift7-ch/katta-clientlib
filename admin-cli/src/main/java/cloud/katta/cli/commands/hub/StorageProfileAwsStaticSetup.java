/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import picocli.CommandLine;

import java.util.List;
import java.util.UUID;

/**
 * Uploads a storage profile to Katta Server for use with AWS static.
 * The storage profile then allows users with <code>create-vaults</code> role to create vaults for an existing AWS S3 bucket.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#storage-profiles">katta docs</a>.
 */
@CommandLine.Command(name = "storageProfileAWSStatic",
        description = "Upload storage profile for AWS Static.",
        mixinStandardHelpOptions = true)
public class StorageProfileAwsStaticSetup extends AbstractStorageProfile {

    @CommandLine.Option(names = {"--region"}, description = "Bucket region, e.g. \"eu-west-1\".", required = true)
    String region;

    @CommandLine.Option(names = {"--regions"}, description = "Bucket regions, e.g. \"--regions eu-west-1  --regions eu-west-2 --regions eu-west-3\"].", required = true)
    List<String> regions;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", required = false, defaultValue = "katta-")
    String bucketPrefix;

    @Override
    protected void call(final UUID uuid, final String name, final ApiClient apiClient) throws ApiException {
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);

        call(uuid, name, storageProfileResourceApi);
    }

    protected void call(final UUID uuid, final String name, final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        storageProfileResourceApi.apiStorageprofileS3staticPost(new StorageProfileS3StaticDto()
                .id(uuid)
                .name(name)
                .protocol(Protocol.S3_STATIC)
                .archived(false)

                // -- (1) bucket creation, template upload and client profile
                .scheme("https")
                .port(443)
                .storageClass(S3STORAGECLASSES.STANDARD)
                .withPathStyleAccessEnabled(false)

                // -- (2) bucket creation only (only relevant for Desktop client)
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                .bucketVersioning(true)
                .region(region)
                .regions(regions)
                .bucketPrefix(bucketPrefix)
                // TODO bad design smell? not all S3 providers might have STS to create static bucket?
                .stsRoleCreateBucketClient("")
                .stsRoleCreateBucketHub("")
        );
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }
}
