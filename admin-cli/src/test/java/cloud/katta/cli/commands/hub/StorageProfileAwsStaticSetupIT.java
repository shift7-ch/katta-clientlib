/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import cloud.katta.cli.KattaSetupCli;
import cloud.katta.cli.commands.CLIIntegrationTest;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.testcontainers.AbtractAdminCliIT;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

@CLIIntegrationTest
class StorageProfileAwsStaticSetupIT extends AbtractAdminCliIT {

    @Test
    public void testStorageProfileAwsStaticSetup() throws Exception {
        final UUID storageProfileId = UUID.randomUUID();
        int rc = new CommandLine(new KattaSetupCli()).execute(
                "storageProfileAWSStatic",
                "--hubUrl", "http://localhost:8280",
                "--accessToken", accessToken,
                "--uuid", storageProfileId.toString(),
                "--name", "AWS S3 Static",
                "--region", "eu-west-1",
                "--regions", "eu-west-1",
                "--regions", "eu-west-2",
                "--regions", "eu-west-3"
        );
        assertEquals(0, rc);
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        Optional<StorageProfileDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream()
                .filter(p -> p.getActualInstance() instanceof StorageProfileS3StaticDto)
                .filter(p -> p.getStorageProfileS3StaticDto().getId().equals(storageProfileId)).findFirst();
        assertTrue(profile.isPresent());
        final StorageProfileS3StaticDto dto = profile.get().getStorageProfileS3StaticDto();
        assertEquals("AWS S3 Static", dto.getName());
        assertEquals(Protocol.S3_STATIC, dto.getProtocol());
        assertFalse(dto.getArchived());
        assertEquals("https", dto.getScheme());
        assertNull(dto.getHostname());
        assertEquals(443, dto.getPort());
        assertFalse(dto.getWithPathStyleAccessEnabled());
        assertEquals(S3STORAGECLASSES.STANDARD, dto.getStorageClass());
        assertEquals("eu-west-1", dto.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"), dto.getRegions());
        assertEquals("katta-", dto.getBucketPrefix());
        assertEquals("", dto.getStsRoleCreateBucketClient());
        assertEquals("", dto.getStsRoleCreateBucketHub());
        assertNull(dto.getStsEndpoint());
        assertTrue(dto.getBucketVersioning());
        assertNull(dto.getBucketAcceleration());
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, dto.getBucketEncryption());
    }
}
