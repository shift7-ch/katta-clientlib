/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.aws;

import java.util.List;
import java.util.UUID;

import cloud.katta.cli.commands.hub.storageprofile.AbstractStorageProfile;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import picocli.CommandLine;

/**
 * Uploads a storage profile to Katta Server for use with AWS static.
 * The storage profile then allows users with <code>create-vaults</code> role to create vaults for an existing AWS S3 bucket.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#storage-profiles">katta docs</a>.
 */
@CommandLine.Command(name = "static",
        description = "Create or upload an AWS static storage profile for an existing S3 bucket.",
        showDefaultValues = true,
        mixinStandardHelpOptions = true)
public class AWSStaticStorageProfile extends AbstractStorageProfile {

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", required = false, defaultValue = "katta-")
    String bucketPrefix;

    public AWSStaticStorageProfile() {
    }

    public AWSStaticStorageProfile(final String hubUrl, final String uuid, final String name, final String region, final List<String> regions, final String bucketPrefix) {
        super(hubUrl, uuid, name, region, regions);
        this.bucketPrefix = bucketPrefix;
    }

    @Override
    protected void call(final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        final UUID uuid = UUID.fromString(null == this.uuid ? UUID.randomUUID().toString() : this.uuid);
        storageProfileResourceApi.apiStorageprofileS3staticPost(new StorageProfileS3StaticDto()
                .id(uuid)
                .name(null == name ? this.toString() : name)
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
                .regions(null == regions ? List.of(region) : regions)
                .bucketPrefix(bucketPrefix)
                // TODO bad design smell? not all S3 providers might have STS to create static bucket?
                .stsRoleCreateBucketClient("")
                .stsRoleCreateBucketHub("")
        );
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }

    @Override
    public String toString() {
        return String.format("AWS (Static) Storage Profile %s", null == regions ? List.of(region) : regions);
    }
}
