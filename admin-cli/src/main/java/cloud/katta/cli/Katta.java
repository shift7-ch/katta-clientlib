/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli;

import cloud.katta.cli.commands.Completion;
import cloud.katta.cli.commands.hub.storageprofile.StorageProfile;
import cloud.katta.cli.commands.login.AuthorizationCodeFlow;
import cloud.katta.cli.commands.storage.Setup;
import picocli.CommandLine;

@CommandLine.Command(name = "katta",
        mixinStandardHelpOptions = true,
        subcommands = {
                // storage
                Setup.class,
                // hub
                StorageProfile.class,
                // misc.
                AuthorizationCodeFlow.class,
                Completion.class,
                CommandLine.HelpCommand.class,
        })
public class Katta {

    public static void main(String... args) {
        int exitCode = new CommandLine(new Katta())
                .setPosixClusteredShortOptionsAllowed(false)
                .execute(args);
        System.exit(exitCode);
    }
}
