/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.minio;

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
import cloud.katta.client.model.StorageProfileS3STSDto;
import picocli.CommandLine;

/**
 * Uploads a storage profile to Katta Server for use with MinIO STS. Requires MinIO STS setup.
 * <p>
 * Unlike AWS, MinIO does not support role chaining (AssumeRole with tagged session).
 * Therefore {@code stsRoleAccessBucketAssumeRoleTaggedSession} and {@code stsSessionTag}
 * are intentionally left {@code null}.
 * <p>
 * MinIO uses the {@code ${jwt:client_id}} policy variable to scope bucket access per vault.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#minio">katta-docs</a>.
 */
@CommandLine.Command(name = "sts",
        description = "Upload storage profile for MinIO STS.",
        showDefaultValues = true,
        mixinStandardHelpOptions = true)
public class MinIOSTSStorageProfile extends AbstractStorageProfile {

    @CommandLine.Option(names = {"--endpointUrl"}, description = "MinIO endpoint URL (S3 API). Example: \"https://minio.example.com\" or \"https://minio.example.com:9000\"", required = true)
    String endpointUrl;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix for STS vaults.", defaultValue = "katta-")
    String bucketPrefix;

    @CommandLine.Option(names = {"--stsRoleCreateBucket"}, description = "MinIO role ARN for bucket creation (from 'mc idp openid ls' for the cryptomator client).", required = true)
    String stsRoleCreateBucket;

    @CommandLine.Option(names = {"--stsRoleAccessBucket"}, description = "MinIO role ARN for bucket access (from 'mc idp openid ls' for the cryptomatorvaults client).", required = true)
    String stsRoleAccessBucket;

    public MinIOSTSStorageProfile() {
    }

    public MinIOSTSStorageProfile(final String hubUrl, final String uuid, final String name, final String region, final List<String> regions,
                                  final String endpointUrl, final String bucketPrefix, final String stsRoleCreateBucket, final String stsRoleAccessBucket) {
        super(hubUrl, uuid, name, region, regions);
        this.endpointUrl = endpointUrl;
        this.bucketPrefix = bucketPrefix;
        this.stsRoleCreateBucket = stsRoleCreateBucket;
        this.stsRoleAccessBucket = stsRoleAccessBucket;
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
        storageProfileResourceApi.apiStorageprofileS3stsPost(new StorageProfileS3STSDto()
                .id(uuid)
                .name(null == name ? this.toString() : name)
                .protocol(Protocol.S3_STS)
                .archived(false)

                // -- (1) S3 endpoint configuration for MinIO
                .scheme(scheme)
                .hostname(hostname)
                .port(port)
                .storageClass(S3STORAGECLASSES.STANDARD)
                .withPathStyleAccessEnabled(true) // Required for MinIO

                // -- (2) bucket creation
                .bucketPrefix(bucketPrefix)
                .region(region)
                .regions(null == regions ? List.of(region) : regions)
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                .bucketVersioning(false) // MinIO versioning is optional
                .bucketAcceleration(null) // Not supported by MinIO

                // -- (3) STS roles from MinIO OIDC setup
                .stsRoleCreateBucketClient(stsRoleCreateBucket)
                .stsRoleCreateBucketHub(stsRoleCreateBucket)
                .stsRoleAccessBucketAssumeRoleWithWebIdentity(stsRoleAccessBucket)

                // -- (4) STS endpoint override for MinIO
                .stsEndpoint(endpointUrl)

                // -- (5) No role chaining for MinIO (AWS-only feature)
                .stsRoleAccessBucketAssumeRoleTaggedSession(null)
                .stsSessionTag(null)
        );
        return storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid);
    }

    @Override
    public String toString() {
        return String.format("MinIO (STS) Storage Profile %s", endpointUrl);
    }
}
