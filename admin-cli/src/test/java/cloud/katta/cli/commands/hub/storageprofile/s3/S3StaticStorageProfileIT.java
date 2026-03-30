/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.s3;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import cloud.katta.cli.Katta;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.testsetup.AbstractAdminCLIIT;
import cloud.katta.testsetup.CLIIntegrationTest;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

@CLIIntegrationTest
class S3StaticStorageProfileIT extends AbstractAdminCLIIT {

    @Test
    public void testStorageProfileS3StaticSetup() throws Exception {
        final UUID storageProfileId = UUID.randomUUID();
        int rc = new CommandLine(new Katta()).execute(
                "storageprofile", "s3", "static",
                "--hubUrl", "http://localhost:8280",
                "--accessToken", accessToken,
                "--uuid", storageProfileId.toString(),
                "--name", "S3 Static",
                "--endpointUrl", "https://s3.example.com",
                "--region", "us-east-1",
                "--regions", "us-east-1",
                "--regions", "us-east-2",
                "--regions", "us-west-1"
        );
        assertEquals(0, rc);
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        Optional<StorageProfileDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream()
                .filter(p -> p.getActualInstance() instanceof StorageProfileS3StaticDto)
                .filter(p -> p.getStorageProfileS3StaticDto().getId().equals(storageProfileId)).findFirst();
        assertTrue(profile.isPresent());
        final StorageProfileS3StaticDto dto = profile.get().getStorageProfileS3StaticDto();
        assertEquals("S3 Static", dto.getName());
        assertEquals(Protocol.S3_STATIC, dto.getProtocol());
        assertFalse(dto.getArchived());
        assertEquals("https", dto.getScheme());
        assertEquals("s3.example.com", dto.getHostname());
        assertEquals(443, dto.getPort());
        assertTrue(dto.getWithPathStyleAccessEnabled());
        assertEquals(S3STORAGECLASSES.STANDARD, dto.getStorageClass());
        assertEquals("us-east-1", dto.getRegion());
        assertEquals(Arrays.asList("us-east-1", "us-east-2", "us-west-1"), dto.getRegions());
        assertEquals("katta-", dto.getBucketPrefix());
        assertEquals("", dto.getStsRoleCreateBucketClient());
        assertEquals("", dto.getStsRoleCreateBucketHub());
        assertNull(dto.getStsEndpoint());
        assertFalse(dto.getBucketVersioning());
        assertNull(dto.getBucketAcceleration());
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, dto.getBucketEncryption());
    }
}
