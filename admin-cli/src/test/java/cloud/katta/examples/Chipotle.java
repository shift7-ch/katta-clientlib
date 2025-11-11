/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.examples;

import cloud.katta.cli.KattaSetupCli;
import picocli.CommandLine;

public class Chipotle {

    public static void main(String[] args) {
        new CommandLine(new KattaSetupCli()).execute(
                "awsSetup",
                "--profileName", "430118840017_AdministratorAccess",
                "--realmUrl", "https://testing.katta.cloud/kc/realms/chipotle",
                "--roleNamePrefix", "testing.katta.cloud-chipotle-",
                "--clientId", "cryptomator",
                "--clientId", "cryptomatorhub",
                "--clientId", "cryptomatorvaults",
                "--bucketPrefix", "katta-test-"
        );
    }
}
