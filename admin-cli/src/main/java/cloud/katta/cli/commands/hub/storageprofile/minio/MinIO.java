/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.minio;

import picocli.CommandLine;

@CommandLine.Command(name = "minio", subcommands = {
        MinioSTSStorageProfile.class,
        CommandLine.HelpCommand.class
},
        description = "Setup MinIO Storage Provider Integration", mixinStandardHelpOptions = true)
public class MinIO {
}
