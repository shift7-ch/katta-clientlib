/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.s3;

import picocli.CommandLine;

@CommandLine.Command(name = "s3", subcommands = {
        S3StaticStorageProfile.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Generic S3 Storage Provider Integration", mixinStandardHelpOptions = true)
public class S3 {
}
