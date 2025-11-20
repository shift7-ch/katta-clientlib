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
import cloud.katta.client.model.StorageProfileS3STSDto;
import picocli.CommandLine;

import java.util.List;
import java.util.UUID;

/**
 * Uploads a storage profile to Katta Server for use with AWS STS. Requires AWS STS setup.
 * <p>
 * The storage profile then allows users with <code>create-vaults</code> role to create vaults and their corresponding S3 bucket seamlessly.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#storage-profiles">katta docs</a>.
 */
@CommandLine.Command(name = "storageProfileAWSSTS",
        description = "Upload storage profile for AWS STS.",
        mixinStandardHelpOptions = true)
public class StorageProfileAwsStsSetup extends AbstractStorageProfile {

    @CommandLine.Option(names = {"--rolePrefix"}, description = "ARN Role Prefix. Example: \"arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-\"", required = true)
    String rolePrefix;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", required = false, defaultValue = "katta-")
    String bucketPrefix;

    @CommandLine.Option(names = {"--region"}, description = "Bucket region, e.g. \"eu-west-1\".", required = true)
    String region;

    @CommandLine.Option(names = {"--regions"}, description = "Bucket regions, e.g. [\"eu-west-1\",\"eu-west-2\",\"eu-west-3\"].", required = true)
    List<String> regions;

    @Override
    protected void call(final UUID uuid, final String name, final ApiClient apiClient) throws ApiException {
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);

        call(uuid, name, storageProfileResourceApi);
    }

    protected void call(final UUID uuid, final String name, final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        storageProfileResourceApi.apiStorageprofileS3stsPost(new StorageProfileS3STSDto()
                .id(uuid)
                .name(name)
                .protocol(Protocol.S3_STS)
                .archived(false)

                // -- (1) bucket creation, template upload and client profile
                .scheme("https")
                .port(443)
                .storageClass(S3STORAGECLASSES.STANDARD)
                .withPathStyleAccessEnabled(false)

                // -- (2) bucket creation only (only relevant for Desktop client)
                .bucketPrefix(bucketPrefix)
                .region(region)
                .regions(regions)
                // TODO extract option with default
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                .bucketVersioning(true)

                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-createbucket
                .stsRoleCreateBucketClient(String.format("%s-createbucket", rolePrefix))
                .stsRoleCreateBucketHub(String.format("%s-createbucket", rolePrefix))
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                // TODO https://github.com/shift7-ch/katta-clientlib/issues/190 naming
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-sts-chain-01
                .stsRoleAccessBucketAssumeRoleWithWebIdentity(String.format("%s-sts-chain-01", rolePrefix))
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-sts-chain-02
                .stsRoleAccessBucketAssumeRoleTaggedSession(String.format("%s-sts-chain-02", rolePrefix))
        );
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }
}
