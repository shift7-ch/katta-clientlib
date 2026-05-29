/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.client.model;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import cloud.katta.client.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Check the generated client code can deserialize `useOneOfDiscriminatorLookup` data.
 */
class ObjectMapperTest {

    @Test
    void testAWSStatic() throws IOException {
        final ObjectMapper mapper = new JSON().getMapper();
        final StorageProfileS3StaticDto profile = mapper.readValue(Objects.requireNonNull(this.getClass().getResourceAsStream(
                "/setup/aws_static/storage_profile.json")), StorageProfileS3StaticDto.class);
        assertEquals(Protocol.S3_STATIC, profile.getProtocol());
        assertNull(profile.getEndpoint());
        // default STANDARD from backend
        assertEquals(S3StorageClass.STANDARD, profile.getStorageClass());
        assertFalse(profile.getArchived());
        assertFalse(profile.getPathStyleAccessEnabled());
    }

    @Test
    void testAWSSTS() throws IOException {
        final ObjectMapper mapper = new JSON().getMapper();
        final StorageProfileS3STSDto profile = mapper.readValue(Objects.requireNonNull(this.getClass().getResourceAsStream(
                "/setup/aws_sts/storage_profile.json")), StorageProfileS3STSDto.class);
        assertEquals("katta-test-", profile.getBucketPrefix());
        assertEquals("eu-west-1", profile.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"), profile.getRegions());
        assertFalse(profile.getPathStyleAccessEnabled());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-chipotle-create-bucket", profile.getStsRoleCreateBucketHub());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-chipotle-create-bucket", profile.getStsRoleCreateBucketClient());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-chipotle-access-bucket-a-role-web-identity", profile.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-chipotle-access-bucket-a-role-tagged-session", profile.getStsRoleAccessBucketAssumeRoleTaggedSession());
        assertEquals(S3StorageClass.STANDARD, profile.getStorageClass());
        assertNull(profile.getArchived());
        assertEquals(Protocol.S3_STS, profile.getProtocol());
        assertNull(profile.getEndpoint());
        // default NONE from backend
        assertNull(profile.getBucketAcceleration());
    }

    @Test
    void testMinioStatic() throws IOException {
        final ObjectMapper mapper = new JSON().getMapper();
        final String minioStaticJson = IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(
                        "/setup/minio_static/storage_profile.json")), StandardCharsets.UTF_8)
                .replace("${MINIO_SCHEME}", "http")
                .replace("${MINIO_HOSTNAME}", "minio")
                .replace("${MINIO_PORT}", "9000");
        final StorageProfileS3StaticDto profile = mapper.readValue(minioStaticJson, StorageProfileS3StaticDto.class);
        assertEquals(Protocol.S3_STATIC, profile.getProtocol());
        assertEquals("http://minio:9000", profile.getEndpoint());
        // default STANDARD from backend
        assertEquals(S3StorageClass.STANDARD, profile.getStorageClass());
        assertFalse(profile.getArchived());
        assertTrue(profile.getPathStyleAccessEnabled());
    }

    @Test
    void testMinioSTS() throws IOException {
        final ObjectMapper mapper = new JSON().getMapper();
        final String minioSTSJson = IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(
                        "/setup/minio_sts/storage_profile.json")), StandardCharsets.UTF_8)
                .replace("${MINIO_SCHEME}", "http")
                .replace("${MINIO_HOSTNAME}", "minio")
                .replace("${MINIO_PORT}", "9000");
        final StorageProfileS3STSDto profile = mapper.readValue(minioSTSJson, StorageProfileS3STSDto.class);
        assertEquals("katta-test-", profile.getBucketPrefix());
        assertEquals("eu-central-1", profile.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3", "eu-north-1", "eu-south-1", "eu-south-2", "eu-central-1", "eu-central-2"), profile.getRegions());
        assertTrue(profile.getPathStyleAccessEnabled());
        assertEquals("arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE", profile.getStsRoleCreateBucketHub());
        assertEquals("arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE", profile.getStsRoleCreateBucketClient());
        assertEquals("arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94", profile.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
        assertNull(profile.getStsRoleAccessBucketAssumeRoleTaggedSession());
        assertEquals(Protocol.S3_STS, profile.getProtocol());
        assertEquals("http://minio:9000", profile.getEndpoint());
        // default NONE from backend
        assertEquals(S3StorageClass.STANDARD, profile.getStorageClass());
        assertNull(profile.getBucketAcceleration());
    }
}
