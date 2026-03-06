/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import picocli.CommandLine;

@CommandLine.Command(name = "storageprofile", subcommands = {
        ArchiveStorageProfile.class,
        cloud.katta.cli.commands.hub.storageprofile.aws.Wrapper.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Storage Provider Integration", mixinStandardHelpOptions = true)
public class Wrapper {
}
