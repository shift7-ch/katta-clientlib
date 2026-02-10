/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.examples;

import java.util.UUID;

import cloud.katta.cli.KattaSetupCli;
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
            new CommandLine(new KattaSetupCli()).execute(
                    "awsSetup",
                    "--profileName", "430118840017_AdministratorAccess",
                    "--realmUrl", realmUrl,
                    "--roleNamePrefix", roleNamePrefix,
                    "--clientId", "cryptomator",
                    "--clientId", "cryptomatorhub",
                    "--clientId", "cryptomatorvaults",
                    "--bucketPrefix", bucketPrefix
            );
        }
        if(true) {
            new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileArchive",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", "929976d0-d359-450a-96c5-9ba3cd581cea"
            );
        }
        if (true) {

            final UUID storageProfileId = UUID.randomUUID();
            new CommandLine(new KattaSetupCli()).execute(
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
        }
    }
}

