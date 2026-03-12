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
import cloud.katta.client.model.StorageProfileS3STSDto;
import picocli.CommandLine;

import static cloud.katta.cli.commands.common.Defaults.*;

/**
 * Uploads a storage profile to Katta Server for use with AWS STS. Requires AWS STS setup.
 * <p>
 * The storage profile then allows users with <code>create-vaults</code> role to create vaults and their corresponding S3 bucket seamlessly.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#storage-profiles">katta docs</a>.
 */
@CommandLine.Command(name = "sts",
        description = "Upload storage profile for AWS STS.",
        mixinStandardHelpOptions = true)
public class AWSSTSStorageProfile extends AbstractStorageProfile {

    @CommandLine.Option(names = {"--rolePrefix"}, description = "ARN Role Prefix. Example: \"arn:aws:iam::<ACCOUNTID>:role/katta-\"", required = true)
    String rolePrefix;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket prefix.", required = false, defaultValue = "katta-")
    String bucketPrefix;

    public AWSSTSStorageProfile() {
    }

    public AWSSTSStorageProfile(final String hubUrl, final String uuid, final String name, final String region, final List<String> regions, final String rolePrefix, final String bucketPrefix) {
        super(hubUrl, uuid, name, region, regions);
        this.rolePrefix = rolePrefix;
        this.bucketPrefix = bucketPrefix;
    }

    @Override
    protected void call(final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        final UUID uuid = UUID.fromString(null == this.uuid ? UUID.randomUUID().toString() : this.uuid);
        storageProfileResourceApi.apiStorageprofileS3stsPost(new StorageProfileS3STSDto()
                .id(uuid)
                .name(null == name ? this.toString() : name)
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
                .regions(null == regions ? List.of(region) : regions)
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                .bucketVersioning(true)

                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-create-bucket
                .stsRoleCreateBucketClient(String.format("%s%s", rolePrefix, CREATE_BUCKET_ROLE_NAME_INFIX))
                .stsRoleCreateBucketHub(String.format("%s%s", rolePrefix, CREATE_BUCKET_ROLE_NAME_INFIX))
                .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-access-bucket-role-web-identity
                .stsRoleAccessBucketAssumeRoleWithWebIdentity(String.format("%s%s%s", rolePrefix, ACCESS_BUCKET_ROLE_NAME_INFIX, ASSUME_ROLE_WITH_WEB_IDENTITY_ROLE_SUFFIX))
                // arn:aws:iam::XXXXXXX:role/testing.katta.cloud-kc-realms-tamarind-access-bucket-role-tagged-session
                .stsRoleAccessBucketAssumeRoleTaggedSession(String.format("%s%s%s", rolePrefix, ACCESS_BUCKET_ROLE_NAME_INFIX, ASSUME_ROLE_TAGGED_SESSION_ROLE_SUFFIX))
                .stsSessionTag(REQUEST_TAG)
        );
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }

    @Override
    public String toString() {
        return String.format("AWS (STS) Storage Profile %s", null == regions ? List.of(region) : regions);
    }
}


