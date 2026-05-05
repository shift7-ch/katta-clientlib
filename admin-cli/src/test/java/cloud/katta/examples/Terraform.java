/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.examples;

import java.util.UUID;

import cloud.katta.cli.Katta;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Terraform example.
 */
public class Terraform {

    public static void main(String[] args) {
        final String workspace = "default";
        final String bucketPrefix = "katta";
        final String roleNamePrefix = "katta";
        final String realmUrl = String.format("https://keycloak.%s.katta.cloud/realms/cryptomator", workspace);
        final String hubUrl = String.format("https://hub.%s.katta.cloud", workspace);
        final String region = "eu-central-1";
        final String tokenUrl = String.format("%s/protocol/openid-connect/token", realmUrl);
        final String authUrl = String.format("%s/protocol/openid-connect/auth", realmUrl);
        if(true) {
            final String[] options = {
                    "setup", "aws",
                    "--profileName", "430118840017_AdministratorAccess",
                    "--realmUrl", realmUrl,
                    "--roleNamePrefix", roleNamePrefix,
                    "--clientId", "cryptomator",
                    "--clientId", "cryptomatorhub",
                    "--clientId", "cryptomatorvaults",
                    "--bucketPrefix", bucketPrefix};
            System.out.println(String.format("katta \"%s\"", String.join("\" \"", options)));
            int rc = new CommandLine(new Katta()).execute(options);
            assertEquals(0, rc);
        }
        if(false) {
            int rc = new CommandLine(new Katta()).execute(
                    "storageprofile", "archive",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", "929976d0-d359-450a-96c5-9ba3cd581cea"
            );
            assertEquals(0, rc);
        }
        if(true) {
            final UUID storageProfileId = UUID.randomUUID();
            final String[] options = {
                    "storageprofile", "aws", "sts",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 STS",
                    "--bucketPrefix", bucketPrefix,
                    "--awsAccountId", "430118840017",
                    "--roleNamePrefix", roleNamePrefix,
                    "--region", region,
                    "--regions", region};
            System.out.println(String.format("katta \"%s\"", String.join("\" \"", options)));
            int rc = new CommandLine(new Katta()).execute(options);
            assertEquals(0, rc);
        }
        if(true) {
            final UUID storageProfileId = UUID.randomUUID();
            final String[] options = {
                    "storageprofile", "s3", "static",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", storageProfileId.toString(),
                    "--name", "S3 Static",
                    "--endpointUrl", "https://s3.example.com",
                    "--region", region,
                    "--regions", region};
            System.out.println(String.format("katta \"%s\"", String.join("\" \"", options)));
            int rc = new CommandLine(new Katta()).execute(options);
            assertEquals(0, rc);
        }
        if(true) {
            final UUID storageProfileId = UUID.randomUUID();
            final String[] options = {
                    "storageprofile", "aws", "static",
                    "--tokenUrl", tokenUrl,
                    "--authUrl", authUrl,
                    "--clientId", "cryptomator",
                    "--hubUrl", hubUrl,
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 Static",
                    "--region", "eu-west-1",
                    "--regions", "eu-west-1",
                    "--regions", "eu-west-2",
                    "--regions", "eu-west-3"};
            System.out.println(String.format("katta \"%s\"", String.join("\" \"", options)));
            int rc = new CommandLine(new Katta()).execute(options);
            assertEquals(0, rc);
        }
    }
}

