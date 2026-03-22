/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import cloud.katta.cli.commands.hub.storageprofile.aws.AWS;
import picocli.CommandLine;

@CommandLine.Command(name = "storageprofile", subcommands = {
        ArchiveStorageProfile.class,
        AWS.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Storage Provider Integration", mixinStandardHelpOptions = true)
public class StorageProfile {
}
