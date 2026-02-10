/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.examples;

import java.util.UUID;

import cloud.katta.cli.KattaSetupCli;
import picocli.CommandLine;

/**
 * Tamarind example.
 */
public class Tamarind {

    public static void main(String[] args) {
        if(false) {
            new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileArchive",
                    "--tokenUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token",
                    "--authUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth",
                    "--clientId", "cryptomator",
                    "--hubUrl", "https://testing.katta.cloud/tamarind/",
                    "--uuid", "d7f8aa61-7b07-423c-89e5-fff8b8c2a56e"
            );
        }

        if(false) {
            final UUID storageProfileId = UUID.randomUUID();
            new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileAWSSTS",
                    "--tokenUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token",
                    "--authUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth",
                    "--clientId", "cryptomator",
                    "--hubUrl", "https://testing.katta.cloud/tamarind/",
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 STS",
                    "--bucketPrefix", "katta-test-",
                    "--rolePrefix", "arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-tamarind"
            );
        }

        if(true) {
            new CommandLine(new KattaSetupCli()).execute(
                    "minioStsSetup",
                    "--endpointUrl", "https://minio.testing.katta.cloud",
                    "--hubUrl", "https://testing.katta.cloud/tamarind",
                    "--roleNamePrefix", "testing.katta.cloud-tamarind-",
                    "--bucketPrefix", "katta-test-tamarind-",
                    "--minioAlias", "minio_testing_katta_cloud",
                    "--accessKey", System.getenv("minio_testing_katta_cloud.access_key"),
                    "--secretKey", System.getenv("minio_testing_katta_cloud.secret_key")
            );
        }
    }
}

