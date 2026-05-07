/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.examples;

import java.util.UUID;

import cloud.katta.cli.Katta;
import picocli.CommandLine;

/**
 * Tamarind example.
 */
public class Tamarind {
    public static void main(String[] args) {
        if(false) {
            new CommandLine(new Katta()).setPosixClusteredShortOptionsAllowed(false).execute(
                    "storageprofile", "archive",
                    "--tokenUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token",
                    "--authUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth",
                    "--clientId", "cryptomator",
                    "--hubUrl", "https://testing.katta.cloud/tamarind/",
                    "--uuid", "d7f8aa61-7b07-423c-89e5-fff8b8c2a56e"
            );
        }

        if(false) {
            final UUID storageProfileId = UUID.randomUUID();
            new CommandLine(new Katta()).setPosixClusteredShortOptionsAllowed(false).execute(
                    "storageprofile", "aws", "sts",
                    "--tokenUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token",
                    "--authUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth",
                    "--clientId", "cryptomator",
                    "--hubUrl", "https://testing.katta.cloud/tamarind/",
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 STS",
                    "--bucketPrefix", "katta-test-",
                    "--awsAccountId", "430118840017",
                    "--roleNamePrefix", "testing.katta.cloud-kc-realms-tamarind-"
            );
        }
        if(false) {
            new CommandLine(new Katta()).setPosixClusteredShortOptionsAllowed(false).execute(
                    "setup", "minio",
                    // N.B. only one idp per client - separate MinIO instances.
                    "--endpointUrl", "https://tamarind.minio.testing.katta.cloud",
                    "--hubUrl", "https://testing.katta.cloud/tamarind",
                    "--roleNamePrefix", "testing.katta.cloud-tamarind-",
                    "--bucketPrefix", "katta-test-tamarind-",
                    "--minioAlias", "tamarind_minio_testing_katta_cloud",
                    "--accessKey", "key",
                    "--secretKey", "secret"
            );
            // mc idp openid ls tamarind_minio_testing_katta_cloud
            //╭───────────────────────────────────────────────────────────────────────────────────────────────────────╮
            //│ On?                       Name                                           RoleARN                      │
            //│ 🔴                                        (default)                                                   │
            //│ 🟢         testing.katta.cloud-tamarind-cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
            //│ 🟢      testing.katta.cloud-tamarind-cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
            //│ 🟢   testing.katta.cloud-tamarind-cryptomatorvaults  arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94 │
            //╰───────────────────────────────────────────────────────────────────────────────────────────────────────╯
            // mc idp openid info tamarind_minio_testing_katta_cloud  testing.katta.cloud-tamarind-cryptomator
            // mc idp openid info tamarind_minio_testing_katta_cloud  testing.katta.cloud-tamarind-cryptomatorhub
            // mc idp openid info tamarind_minio_testing_katta_cloud  testing.katta.cloud-tamarind-cryptomatorvaults
        }
        if(false) {
            new CommandLine(new Katta()).setPosixClusteredShortOptionsAllowed(false).execute(
                    "storageprofile", "s3", "static",
                    "--hubUrl", "https://testing.katta.cloud/tamarind",
                    "--name", "MinIO S3 Static",
                    "--endpointUrl", "https://tamarind.minio.testing.katta.cloud",
                    "--bucketPrefix", "katta-test-tamarind-",
                    "--region", "eu-west-1",
                    "--regions", "eu-west-1",
                    "--regions", "eu-west-2",
                    "--regions", "eu-west-3"
            );
        }
        if(false) {
            new CommandLine(new Katta()).setPosixClusteredShortOptionsAllowed(false).execute(
                    "storageprofile", "minio", "sts",
                    "--endpointUrl", "https://tamarind.minio.testing.katta.cloud",
                    "--hubUrl", "https://testing.katta.cloud/tamarind",
                    "--bucketPrefix", "katta-test-tamarind-",
                    "--region", "eu-west-3",
                    "--regions", "eu-west-3",
                    "--stsRoleCreateBucketClient", "arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE",
                    "--stsRoleCreateBucketHub", "arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE",
                    "--stsRoleAccessBucket", "arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94"
            );
        }
    }
}
