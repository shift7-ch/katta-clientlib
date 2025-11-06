/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

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
                    "--uuid", "d7f8aa61-7b07-423c-89e5-fff8b8c2a56e",
                    // TODO --name should not be required
                    "--name", "ignore-me"
            );
        }

        if(true) {
            final UUID storageProfileId = UUID.randomUUID();
            new CommandLine(new KattaSetupCli()).execute(
                    "storageProfileAWSSTS",
                    "--tokenUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token",
                    "--authUrl", "https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth",
                    "--clientId", "cryptomator",
                    "--hubUrl", "https://testing.katta.cloud/tamarind/",
                    "--uuid", storageProfileId.toString(),
                    "--name", "AWS S3 STS",
                    // TODO inconsistency w/ vs w/o hyphen
                    "--bucketPrefix", "katta-test-",
                    "--rolePrefix", "arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-tamarind"
            );
        }
    }
}

