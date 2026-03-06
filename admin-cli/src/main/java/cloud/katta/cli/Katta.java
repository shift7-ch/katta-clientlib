/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli;

import cloud.katta.cli.commands.login.AuthorizationCodeFlow;
import picocli.CommandLine;

@CommandLine.Command(name = "katta",
        mixinStandardHelpOptions = true,
        subcommands = {
                // storage
                cloud.katta.cli.commands.storage.Wrapper.class,
                // hub
                cloud.katta.cli.commands.hub.storageprofile.Wrapper.class,
                // misc.
                AuthorizationCodeFlow.class,
                CommandLine.HelpCommand.class,
        })
public class Katta {

    public static void main(String... args) {
        var app = new Katta();
        int exitCode = new CommandLine(app)
                .setPosixClusteredShortOptionsAllowed(false)
                .execute(args);
        System.exit(exitCode);
    }
}
