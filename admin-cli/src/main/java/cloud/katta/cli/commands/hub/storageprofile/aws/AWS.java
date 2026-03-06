/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.aws;

import picocli.CommandLine;

@CommandLine.Command(name = "aws", subcommands = {
        AWSStaticStorageProfile.class,
        AWSSTSStorageProfile.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Storage Provider Integration", mixinStandardHelpOptions = true)
public class AWS {
}
