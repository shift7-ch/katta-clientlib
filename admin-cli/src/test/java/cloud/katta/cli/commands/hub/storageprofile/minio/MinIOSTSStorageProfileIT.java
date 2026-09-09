/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.minio;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import cloud.katta.cli.Katta;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.testsetup.AbstractAdminCLIIT;
import cloud.katta.testsetup.CLIIntegrationTest;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unlike {@link cloud.katta.cli.commands.hub.storageprofile.aws.AWSSTSStorageProfileIT}, this profile carries an
 * endpoint and an STS endpoint. Reading it back exercises the corresponding setters on
 * {@link StorageProfileS3STSDto}, which the native-image agent needs to observe to write them to the reachability
 * metadata.
 */
@CLIIntegrationTest
class MinIOSTSStorageProfileIT extends AbstractAdminCLIIT {

    @Test
    public void testStorageProfileMinIOStsSetup() throws Exception {
        final String profileName = "MinIO STS - " + UUID.randomUUID();
        int rc = new CommandLine(new Katta()).execute(
                "storageprofile", "minio", "sts",
                "--hubUrl", "http://localhost:8280",
                "--accessToken", accessToken,
                "--name", profileName,
                "--endpointUrl", "https://minio.example.com:9000",
                "--stsRoleCreateBucketClient", "arn:minio:iam:::role/rigatoni-create-bucket-client",
                "--stsRoleCreateBucketHub", "arn:minio:iam:::role/rigatoni-create-bucket-hub",
                "--stsRoleAccessBucket", "arn:minio:iam:::role/rigatoni-access-bucket",
                "--region", "us-east-1",
                "--regions", "us-east-1",
                "--regions", "us-east-2",
                "--regions", "us-west-1"
        );
        assertEquals(0, rc);
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        Optional<StorageProfileDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream()
                .filter(p -> p.getActualInstance() instanceof StorageProfileS3STSDto)
                .filter(p -> p.getStorageProfileS3STSDto().getName().equals(profileName)).findFirst();
        assertTrue(profile.isPresent());
        final StorageProfileS3STSDto dto = profile.get().getStorageProfileS3STSDto();
        assertEquals(profileName, dto.getName());
        assertEquals(Protocol.S3_STS, dto.getProtocol());
        assertFalse(dto.getArchived());
        assertEquals("https://minio.example.com:9000", dto.getEndpoint());
        assertTrue(dto.getPathStyleAccessEnabled());
        assertEquals(S3StorageClass.STANDARD, dto.getStorageClass());
        assertEquals("us-east-1", dto.getRegion());
        assertEquals(Arrays.asList("us-east-1", "us-east-2", "us-west-1"), dto.getRegions());
        assertEquals("katta-", dto.getBucketPrefix());
        assertEquals("arn:minio:iam:::role/rigatoni-create-bucket-client", dto.getStsRoleCreateBucketClient());
        assertEquals("arn:minio:iam:::role/rigatoni-create-bucket-hub", dto.getStsRoleCreateBucketHub());
        assertEquals("https://minio.example.com:9000", dto.getStsEndpoint());
        assertEquals("arn:minio:iam:::role/rigatoni-access-bucket",
                dto.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
        assertNull(dto.getStsRoleAccessBucketAssumeRoleTaggedSession());
        assertNull(dto.getStsDurationSeconds());
        assertEquals("Vault", dto.getStsSessionTag());
    }
}
