/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import java.util.Arrays;
import java.util.UUID;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3STSDto;
import picocli.CommandLine;

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

    @CommandLine.Option(names = {"--rolePrefix"}, description = "ARN Role Prefix. Example: \"arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind\"", required = true)
    String rolePrefix;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", required = false, defaultValue = "katta")
    String bucketPrefix;

    @Override
    protected void call(final UUID uuid, final String name, final ApiClient apiClient) throws ApiException {
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);

        call(uuid, name, storageProfileResourceApi);
    }

    protected void call(final UUID uuid, final String name, final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        storageProfileResourceApi.apiStorageprofileS3stsPost(new StorageProfileS3STSDto()
                .id(uuid)
                .name(name)
                .bucketPrefix(bucketPrefix)
                .protocol(Protocol.S3_STS)
                .storageClass(S3STORAGECLASSES.STANDARD)
                .region("eu-west-1")
                .regions(Arrays.asList("eu-west-1",
                        "eu-west-2",
                        "eu-west-3"))
                .archived(false)
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-sts-chain-01
                .stsRoleAccessBucketAssumeRoleWithWebIdentity(String.format("%s-sts-chain-01", rolePrefix))
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-sts-chain-02
                .stsRoleAccessBucketAssumeRoleTaggedSession(String.format("%s-sts-chain-02", rolePrefix))
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-createbucket
                .stsRoleCreateBucketClient(String.format("%s-createbucket", rolePrefix))
                .stsRoleCreateBucketHub(String.format("%s-createbucket", rolePrefix))
                .scheme("https")
                .port(443)
        );
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }
}
