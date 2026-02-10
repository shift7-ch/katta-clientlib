/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.examples;

import java.util.UUID;

import cloud.katta.cli.KattaSetupCli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import picocli.CommandLine;

/**
 * Terraform example.
 */
public class Terraform {
    private static final String workspace = "che";

    public static void main(String[] args) {
        final String bucketPrefix = String.format("hub-%s-catta-cloud-", workspace);
        final String roleNamePrefix = String.format("hub.%s.catta.cloud-", workspace);
        final String realmUrl = String.format("https://keycloak.%s.catta.cloud/realms/cryptomator", workspace);
        final String hubUrl = String.format("https://hub.%s.catta.cloud", workspace);
        final String region = "eu-central-1";
        final String tokenUrl = String.format("%s/protocol/openid-connect/token", realmUrl);
        final String authUrl = String.format("%s/protocol/openid-connect/auth", realmUrl);
        if(true) {
            int rc = new CommandLine(new KattaSetupCli()).execute(
                    "awsSetup",
                    "--profileName", "430118840017_AdministratorAccess",
                    "--realmUrl", realmUrl,
                    "--roleNamePrefix", roleNamePrefix,
                    "--clientId", "cryptomator",
                    "--clientId", "cryptomatorhub",
                    "--clientId", "cryptomatorvaults",
                    "--bucketPrefix", bucketPrefix
            );
            assertEquals(0, rc);
        }
        if(false) {
            int rc = new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileArchive",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", "929976d0-d359-450a-96c5-9ba3cd581cea"
            );
            assertEquals(0, rc);
        }
        if (true) {
            final UUID storageProfileId = UUID.randomUUID();
            int rc = new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileAWSSTS",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 STS",
                    "--bucketPrefix", bucketPrefix,
                    "--rolePrefix", String.format("arn:aws:iam::430118840017:role/%s", roleNamePrefix),
                    "--region", region,
                    "--regions", region
            );
            assertEquals(0, rc);
        }
        if (true){
            final UUID storageProfileId = UUID.randomUUID();
            int rc = new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileAWSStatic",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 Static",
                    "--region", "eu-west-1",
                    "--regions", "eu-west-1",
                    "--regions", "eu-west-2",
                    "--regions", "eu-west-3"
            );
            assertEquals(0, rc);
        }
    }
}

