/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.minio;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.JSON;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3STSDto;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;

class MinIOSTSStorageProfileTest {

    @Test
    public void testCall() throws ApiException {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final UUID profileId = UUID.randomUUID();
        final MinIOSTSStorageProfile cli = new MinIOSTSStorageProfile(null, profileId.toString(), "MinIO STS", "us-east-1",
                Arrays.asList("us-east-1", "us-west-2"),
                "https://minio.example.com:9000", "katta-",
                "arn:minio:iam:::role/fusilli-create-bucket-client",
                "arn:minio:iam:::role/fusilli-create-bucket-hub",
                "arn:minio:iam:::role/fusilli-access-bucket");
        cli.call(api);

        final StorageProfileS3STSDto dto = new StorageProfileS3STSDto();
        dto.setId(profileId);
        dto.setName("MinIO STS");
        dto.setProtocol(Protocol.S3_STS);
        dto.setArchived(false);
        dto.setScheme("https");
        dto.setHostname("minio.example.com");
        dto.setPort(9000);
        dto.setStorageClass(S3STORAGECLASSES.STANDARD);
        dto.setWithPathStyleAccessEnabled(true);
        dto.setBucketPrefix("katta-");
        dto.setRegion("us-east-1");
        dto.setRegions(Arrays.asList("us-east-1", "us-west-2"));
        dto.setBucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
        dto.setBucketVersioning(false);
        dto.setBucketAcceleration(null);
        dto.setStsRoleCreateBucketClient("arn:minio:iam:::role/fusilli-create-bucket-client");
        dto.setStsRoleCreateBucketHub("arn:minio:iam:::role/fusilli-create-bucket-hub");
        dto.setStsRoleAccessBucketAssumeRoleWithWebIdentity("arn:minio:iam:::role/fusilli-access-bucket");
        dto.setStsEndpoint("https://minio.example.com:9000");
        dto.setStsRoleAccessBucketAssumeRoleTaggedSession(null);
        dto.setStsSessionTag(null);
        Mockito.verify(api, times(1)).apiStorageprofileS3stsPost(dto);
    }

    @Test
    public void testCallDefaultPortHttps() throws Exception {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final UUID profileId = UUID.randomUUID();
        final MinIOSTSStorageProfile cli = new MinIOSTSStorageProfile(null, profileId.toString(), "MinIO STS", "us-east-1", null,
                "https://minio.example.com", "katta-",
                "arn:minio:iam:::role/create-bucket-client",
                "arn:minio:iam:::role/create-bucket-hub",
                "arn:minio:iam:::role/access-bucket");
        cli.call(api);

        final StorageProfileS3STSDto dto = new StorageProfileS3STSDto();
        dto.setId(profileId);
        dto.setName("MinIO STS");
        dto.setProtocol(Protocol.S3_STS);
        dto.setArchived(false);
        dto.setScheme("https");
        dto.setHostname("minio.example.com");
        dto.setPort(443);
        dto.setStorageClass(S3STORAGECLASSES.STANDARD);
        dto.setWithPathStyleAccessEnabled(true);
        dto.setBucketPrefix("katta-");
        dto.setRegion("us-east-1");
        dto.setRegions(Arrays.asList("us-east-1"));
        dto.setBucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
        dto.setBucketVersioning(false);
        dto.setBucketAcceleration(null);
        dto.setStsRoleCreateBucketClient("arn:minio:iam:::role/create-bucket-client");
        dto.setStsRoleCreateBucketHub("arn:minio:iam:::role/create-bucket-hub");
        dto.setStsRoleAccessBucketAssumeRoleWithWebIdentity("arn:minio:iam:::role/access-bucket");
        dto.setStsEndpoint("https://minio.example.com");
        dto.setStsRoleAccessBucketAssumeRoleTaggedSession(null);
        dto.setStsSessionTag(null);
        Mockito.verify(api, times(1)).apiStorageprofileS3stsPost(dto);
        assertNotEquals("{}", new JSON().getMapper().writeValueAsString(dto));
    }
}
