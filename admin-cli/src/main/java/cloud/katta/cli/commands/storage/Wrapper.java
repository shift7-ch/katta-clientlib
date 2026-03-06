/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage;

import cloud.katta.cli.commands.storage.aws.AWSSTSStorage;
import cloud.katta.cli.commands.storage.minio.MinioSTSStorage;
import picocli.CommandLine;

@CommandLine.Command(name = "setup", subcommands = {
        AWSSTSStorage.class,
        MinioSTSStorage.class,
        CommandLine.HelpCommand.class
},
        description = "Setup Storage Provider Integration", mixinStandardHelpOptions = true)
public class Wrapper {
}
