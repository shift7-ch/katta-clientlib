/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.s3;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import cloud.katta.cli.commands.hub.storageprofile.AbstractStorageProfile;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import picocli.CommandLine;

/**
 * Uploads a storage profile to Katta Server for use with a generic S3-compatible provider using static access tokens.
 * Unlike STS-based profiles, no temporary credentials are requested; the server uses static access key credentials.
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
        final URI uri;
        try {
            uri = new URI(endpointUrl);
        }
        catch(URISyntaxException e) {
            throw new IllegalArgumentException("Invalid endpoint URL: " + endpointUrl, e);
        }
        final String scheme = uri.getScheme();
        final String hostname = uri.getHost();
        final int port = uri.getPort() == -1 ? ("https".equals(scheme) ? 443 : 80) : uri.getPort();
        storageProfileResourceApi.apiStorageprofileS3staticPost(new StorageProfileS3StaticDto()
                .id(uuid)
                .name(null == name ? this.toString() : name)
                .protocol(Protocol.S3_STATIC)
                .archived(false)

                .scheme(scheme)
                .hostname(hostname)
                .port(port)
                .storageClass(S3STORAGECLASSES.STANDARD)
                .withPathStyleAccessEnabled(true) // Required for generic S3-compatible providers

                .bucketPrefix(bucketPrefix)
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                .bucketVersioning(false)
                .bucketAcceleration(null) // Not supported by generic S3 providers

                .region(region)
                .regions(null == regions ? List.of(region) : regions)

                // Workaround https://github.com/shift7-ch/katta-server/issues/124
                .stsRoleCreateBucketClient("")
                .stsRoleCreateBucketHub("")
                .stsEndpoint(null)
        );
        return storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid);
    }

    @Override
    public String toString() {
        return String.format("S3 (Static) Storage Profile %s", endpointUrl);
    }
}
