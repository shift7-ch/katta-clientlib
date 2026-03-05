/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import picocli.CommandLine;

@CommandLine.Command(name = "storageprofile", subcommands = {
        StorageProfileArchive.class,
        StorageProfileAwsSetup.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Storage Provider Integration", mixinStandardHelpOptions = true)
public class StorageProfile {
}
