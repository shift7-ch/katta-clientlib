/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import cloud.katta.cli.commands.hub.storageprofile.aws.AWS;
import cloud.katta.cli.commands.hub.storageprofile.minio.MinIO;
import cloud.katta.cli.commands.hub.storageprofile.s3.S3;
import picocli.CommandLine;

@CommandLine.Command(name = "storageprofile", subcommands = {
        ArchiveStorageProfile.class,
        AWS.class,
        MinIO.class,
        S3.class,
        CommandLine.HelpCommand.class
},
        description = "Configure Storage Location", mixinStandardHelpOptions = true)
public class StorageProfile {
}
