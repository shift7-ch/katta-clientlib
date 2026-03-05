/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage;

import picocli.CommandLine;

@CommandLine.Command(name = "setup", subcommands = {
        AwsStsSetup.class,
        MinioStsSetup.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Storage Provider Integration", mixinStandardHelpOptions = true)
public class Setup {
}
