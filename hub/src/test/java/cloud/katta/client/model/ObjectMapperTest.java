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
public class ObjectMapperTest {

    @Test
    public void testAWSStatic() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        final StorageProfileS3Dto awsStaticProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/aws_static/aws_static_profile.json"), StorageProfileS3Dto.class);
        assertEquals(Protocol.S3, awsStaticProfile.getProtocol());
        assertEquals("https", awsStaticProfile.getScheme());
        assertNull(awsStaticProfile.getHostname());
        assertEquals(443, awsStaticProfile.getPort());
        // default STANDARD from backend
        assertEquals(S3STORAGECLASSES.STANDARD, awsStaticProfile.getStorageClass());
        assertFalse(awsStaticProfile.getArchived());
        assertFalse(awsStaticProfile.getWithPathStyleAccessEnabled());
    }

    @Test
    public void testAWSSTS() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());


        final StorageProfileS3STSDto awsSTSProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/aws_sts/aws_sts_profile.json"), StorageProfileS3STSDto.class);
        assertEquals("katta", awsSTSProfile.getBucketPrefix());
        assertEquals("eu-west-1", awsSTSProfile.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"), awsSTSProfile.getRegions());
        assertFalse(awsSTSProfile.getWithPathStyleAccessEnabled());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket", awsSTSProfile.getStsRoleArnHub());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-createbucket", awsSTSProfile.getStsRoleArnClient());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-01", awsSTSProfile.getStsRoleArn());
        assertEquals("arn:aws:iam::430118840017:role/testing.katta.cloud-kc-realms-chipotle-sts-chain-02", awsSTSProfile.getStsRoleArn2());
        assertEquals(Protocol.S3_STS, awsSTSProfile.getProtocol());
        assertNull(awsSTSProfile.getScheme());
        assertNull(awsSTSProfile.getHostname());
        assertNull(awsSTSProfile.getPort());
        // default NONE from backend
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, awsSTSProfile.getBucketEncryption());
        assertNull(awsSTSProfile.getBucketAcceleration());
    }

    @Test
    public void testMinioStatic() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        final StorageProfileS3Dto minioStaticProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/minio_static/minio_static_profile.json"), StorageProfileS3Dto.class);
        assertEquals(Protocol.S3, minioStaticProfile.getProtocol());
        assertEquals("http", minioStaticProfile.getScheme());
        assertEquals("minio", minioStaticProfile.getHostname());
        assertEquals(9000, minioStaticProfile.getPort());
        // default STANDARD from backend
        assertEquals(S3STORAGECLASSES.STANDARD, minioStaticProfile.getStorageClass());
        assertFalse(minioStaticProfile.getArchived());
        assertTrue(minioStaticProfile.getWithPathStyleAccessEnabled());
    }

    @Test
    public void testMinioSTS() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        final StorageProfileS3STSDto minioSTSProfile = mapper.readValue(this.getClass().getResourceAsStream("/setup/minio_sts/minio_sts_profile.json"), StorageProfileS3STSDto.class);
        assertEquals("katta", minioSTSProfile.getBucketPrefix());
        assertEquals("eu-central-1", minioSTSProfile.getRegion());
        assertEquals(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3", "eu-north-1", "eu-south-1", "eu-south-2", "eu-central-1", "eu-central-2"), minioSTSProfile.getRegions());
        assertTrue(minioSTSProfile.getWithPathStyleAccessEnabled());
        assertEquals("arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE", minioSTSProfile.getStsRoleArnHub());
        assertEquals("arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE", minioSTSProfile.getStsRoleArnClient());
        assertEquals("arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94", minioSTSProfile.getStsRoleArn());
        assertNull(minioSTSProfile.getStsRoleArn2());
        assertEquals(Protocol.S3_STS, minioSTSProfile.getProtocol());
        assertEquals("http", minioSTSProfile.getScheme());
        assertEquals("minio", minioSTSProfile.getHostname());
        assertEquals(9000, minioSTSProfile.getPort());
        // default NONE from backend
        assertEquals(S3SERVERSIDEENCRYPTION.NONE, minioSTSProfile.getBucketEncryption());
        assertNull(minioSTSProfile.getBucketAcceleration());
    }
}
