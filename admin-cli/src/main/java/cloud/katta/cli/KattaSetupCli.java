/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli;

import cloud.katta.cli.commands.AuthorizationCode;
import cloud.katta.cli.commands.AwsStsSetup;
import cloud.katta.cli.commands.StorageProfileArchive;
import cloud.katta.cli.commands.StorageProfileAwsStaticSetup;
import cloud.katta.cli.commands.StorageProfileAwsStsSetup;
import picocli.CommandLine;

@CommandLine.Command(name = "katta-admin-cli",
        mixinStandardHelpOptions = true,
        subcommands = {
                AwsStsSetup.class, CommandLine.HelpCommand.class, StorageProfileAwsStsSetup.class, StorageProfileAwsStaticSetup.class,
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
