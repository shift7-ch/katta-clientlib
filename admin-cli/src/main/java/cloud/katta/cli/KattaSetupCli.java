/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli;

import cloud.katta.cli.commands.*;
import picocli.CommandLine;

@CommandLine.Command(name = "katta-admin-cli",
        mixinStandardHelpOptions = true,
        subcommands = {
                AwsSTSSetup.class, CommandLine.HelpCommand.class, StorageProfileAWSSTSSetup.class, StorageProfileAWSStaticSetup.class,
                AuthorizationCode.class,
                StorageProfileArchive.class
        })
public class KattaSetupCli {

    public static void main(String... args) {
        var app = new KattaSetupCli();
        int exitCode = new CommandLine(app)
                .setPosixClusteredShortOptionsAllowed(false)
                .execute(args);
        System.exit(exitCode);
    }
}
