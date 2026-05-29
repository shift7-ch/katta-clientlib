/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.s3;

import java.util.List;
import java.util.UUID;

import cloud.katta.cli.commands.hub.storageprofile.AbstractStorageProfile;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import picocli.CommandLine;

/**
 * Uploads a storage profile to Katta Server for use with a generic S3-compatible provider using static access tokens.
 * Unlike STS-based profiles, no temporary credentials are requested; we use static access key credentials to access S3 storage.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#storage-profiles">katta docs</a>.
 */
@CommandLine.Command(name = "static",
        description = "Upload storage profile for a generic S3-compatible provider with static access tokens.",
        showDefaultValues = true,
        mixinStandardHelpOptions = true)
public class S3StaticStorageProfile extends AbstractStorageProfile {

    @CommandLine.Option(names = {"--endpointUrl"}, description = "S3 endpoint URL. Example: \"https://s3.example.com\" or \"https://s3.example.com:9000\"", required = true)
    String endpointUrl;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", defaultValue = "katta-")
    String bucketPrefix;

    public S3StaticStorageProfile() {
    }

    public S3StaticStorageProfile(final String hubUrl, final String uuid, final String name, final String region, final List<String> regions,
                                  final String endpointUrl, final String bucketPrefix) {
        super(hubUrl, uuid, name, region, regions);
        this.endpointUrl = endpointUrl;
        this.bucketPrefix = bucketPrefix;
    }

    @Override
    protected StorageProfileDto call(final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        final UUID uuid = UUID.fromString(null == this.uuid ? UUID.randomUUID().toString() : this.uuid);

        storageProfileResourceApi.apiStorageprofilePost(new StorageProfileDto(new StorageProfileS3StaticDto(uuid)
                .name(null == name ? this.toString() : name)
                .protocol(Protocol.S3_STATIC)
                .archived(false)

                .endpoint(endpointUrl)
                .storageClass(S3StorageClass.STANDARD)
                .pathStyleAccessEnabled(true) // Required for generic S3-compatible providers

                .bucketPrefix(bucketPrefix)
                // TODO missing static - required for bucket creation
//                .bucketVersioning(false)
//                .bucketAcceleration(null) // Not supported by generic S3 providers

                .region(region)
                .regions(null == regions ? List.of(region) : regions)
        ));
        return storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid);
    }

    @Override
    public String toString() {
        return String.format("S3 (Static) Storage Profile %s", endpointUrl);
    }
}
