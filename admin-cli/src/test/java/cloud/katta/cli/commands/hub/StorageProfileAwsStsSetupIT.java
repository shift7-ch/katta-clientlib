/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import cloud.katta.cli.KattaSetupCli;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.*;
import cloud.katta.testcontainers.AbtractAdminCliIT;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

class StorageProfileAwsStsSetupIT extends AbtractAdminCliIT {

    @Test
    public void testStorageProfileAwsStsSetup() throws Exception {
        final UUID storageProfileId = UUID.randomUUID();
        int rc = new CommandLine(new KattaSetupCli()).execute(
                "storageProfileAWSSTS",
                "--hubUrl", "http://localhost:8280",
                "--accessToken", accessToken,
                "--uuid", storageProfileId.toString(),
                "--name", "AWS S3 STS",
                "--rolePrefix", "arn:aws:iam::linguine:role/farfalle",
                "--region", "eu-west-1",
                "--regions", "eu-west-1",
                "--regions", "eu-west-2",
                "--regions", "eu-west-3"
        );
        assertEquals(0, rc);
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        Optional<StorageProfileDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream()
                .filter(p -> p.getActualInstance() instanceof StorageProfileS3STSDto)
                .filter(p -> p.getStorageProfileS3STSDto().getId().equals(storageProfileId)).findFirst();
        assertTrue(profile.isPresent());
        final StorageProfileS3STSDto dto = profile.get().getStorageProfileS3STSDto();
        assertEquals("AWS S3 STS", dto.getName());
        assertEquals(Protocol.S3_STS, dto.getProtocol());
        assertFalse(dto.getArchived());
        assertEquals("AWS S3 STS", dto.getName());
        assertEquals("https", dto.getScheme());
        assertNull(dto.getHostname());
        assertEquals(443, dto.getPort());
        assertFalse(dto.getWithPathStyleAccessEnabled());
        assertEquals(S3STORAGECLASSES.STANDARD, dto.getStorageClass());
        assertEquals("eu-west-1", dto.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"), dto.getRegions());
        assertEquals("katta", dto.getBucketPrefix());
        assertEquals("katta", dto.getBucketPrefix());
        assertEquals("arn:aws:iam::linguine:role/farfalle-createbucket", dto.getStsRoleCreateBucketClient());
        assertEquals("arn:aws:iam::linguine:role/farfalle-createbucket", dto.getStsRoleCreateBucketHub());
        assertNull(dto.getStsEndpoint());
        assertTrue(dto.getBucketVersioning());
        assertNull(dto.getBucketAcceleration());
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, dto.getBucketEncryption());
        assertEquals("arn:aws:iam::linguine:role/farfalle-sts-chain-01", dto.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
        assertEquals("arn:aws:iam::linguine:role/farfalle-sts-chain-02", dto.getStsRoleAccessBucketAssumeRoleTaggedSession());
        assertNull(dto.getStsDurationSeconds());
    }
}
