/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.client.model;

import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Check the generated client code can deserialize `useOneOfDiscriminatorLookup` data.
 */
class ObjectMapperTest {

    @Test
    void testAWSStatic() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        final StorageProfileS3StaticDto awsStaticProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/hybrid/aws_static/storage_profile.json"), StorageProfileS3StaticDto.class);
        assertEquals(Protocol.S3_STATIC, awsStaticProfile.getProtocol());
        assertEquals("https", awsStaticProfile.getScheme());
        assertNull(awsStaticProfile.getHostname());
        assertEquals(443, awsStaticProfile.getPort());
        // default STANDARD from backend
        assertEquals(S3STORAGECLASSES.STANDARD, awsStaticProfile.getStorageClass());
        assertFalse(awsStaticProfile.getArchived());
        assertFalse(awsStaticProfile.getWithPathStyleAccessEnabled());
    }

    @Test
    void testAWSSTS() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());


        final StorageProfileS3STSDto awsSTSProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/hybrid/aws_sts/storage_profile.json"), StorageProfileS3STSDto.class);
        assertEquals("katta-test-", awsSTSProfile.getBucketPrefix());
        assertEquals("eu-west-1", awsSTSProfile.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"), awsSTSProfile.getRegions());
        assertFalse(awsSTSProfile.getWithPathStyleAccessEnabled());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket", awsSTSProfile.getStsRoleCreateBucketHub());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket", awsSTSProfile.getStsRoleCreateBucketClient());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", awsSTSProfile.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02", awsSTSProfile.getStsRoleAccessBucketAssumeRoleTaggedSession());
        assertEquals(Protocol.S3_STS, awsSTSProfile.getProtocol());
        assertNull(awsSTSProfile.getScheme());
        assertNull(awsSTSProfile.getHostname());
        assertNull(awsSTSProfile.getPort());
        // default NONE from backend
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, awsSTSProfile.getBucketEncryption());
        assertNull(awsSTSProfile.getBucketAcceleration());
    }

    @Test
    void testMinioStatic() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        final StorageProfileS3StaticDto minioStaticProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/local/minio_static/storage_profile.json"), StorageProfileS3StaticDto.class);
        assertEquals(Protocol.S3_STATIC, minioStaticProfile.getProtocol());
        assertEquals("http", minioStaticProfile.getScheme());
        assertEquals("minio", minioStaticProfile.getHostname());
        assertEquals(9000, minioStaticProfile.getPort());
        // default STANDARD from backend
        assertEquals(S3STORAGECLASSES.STANDARD, minioStaticProfile.getStorageClass());
        assertFalse(minioStaticProfile.getArchived());
        assertTrue(minioStaticProfile.getWithPathStyleAccessEnabled());
    }

    @Test
    void testMinioSTS() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        final StorageProfileS3STSDto minioSTSProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/local/minio_sts/storage_profile.json"), StorageProfileS3STSDto.class);
        assertEquals("katta-test-", minioSTSProfile.getBucketPrefix());
        assertEquals("eu-central-1", minioSTSProfile.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3", "eu-north-1", "eu-south-1", "eu-south-2", "eu-central-1", "eu-central-2"), minioSTSProfile.getRegions());
        assertTrue(minioSTSProfile.getWithPathStyleAccessEnabled());
        assertEquals("arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE", minioSTSProfile.getStsRoleCreateBucketHub());
        assertEquals("arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE", minioSTSProfile.getStsRoleCreateBucketClient());
        assertEquals("arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94", minioSTSProfile.getStsRoleAccessBucketAssumeRoleWithWebIdentity());
        assertNull(minioSTSProfile.getStsRoleAccessBucketAssumeRoleTaggedSession());
        assertEquals(Protocol.S3_STS, minioSTSProfile.getProtocol());
        assertEquals("http", minioSTSProfile.getScheme());
        assertEquals("minio", minioSTSProfile.getHostname());
        assertEquals(9000, minioSTSProfile.getPort());
        // default NONE from backend
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, minioSTSProfile.getBucketEncryption());
        assertNull(minioSTSProfile.getBucketAcceleration());
    }
}
