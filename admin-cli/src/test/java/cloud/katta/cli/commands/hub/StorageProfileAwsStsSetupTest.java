/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3STSDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class StorageProfileAwsStsSetupTest {
    @Test
    public void testCall() throws ApiException {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final UUID vaultId = UUID.randomUUID();
        final StorageProfileAwsStsSetup cli = new StorageProfileAwsStsSetup();
        cli.bucketPrefix = "fancy";
        cli.rolePrefix = "arn:aws:iam::1234:role/testing.katta.cloud-kc-realms-pepper";
        cli.call(vaultId, "AWS S3 STS", api);

        final StorageProfileS3STSDto dto = new StorageProfileS3STSDto();
        dto.setId(vaultId);
        dto.setName("AWS S3 STS");
        dto.setProtocol(Protocol.S3_STS);
        dto.setArchived(false);
        dto.setScheme("https");
        dto.setPort(443);
        dto.setWithPathStyleAccessEnabled(false);
        dto.setStorageClass(S3STORAGECLASSES.STANDARD);
        dto.setRegion("eu-west-1");
        dto.setRegions(Arrays.asList("eu-west-1", "eu-west-2", "eu-west-3"));
        dto.bucketPrefix("fancy");
        dto.stsRoleCreateBucketClient("arn:aws:iam::1234:role/testing.katta.cloud-kc-realms-pepper-createbucket");
        dto.stsRoleCreateBucketHub("arn:aws:iam::1234:role/testing.katta.cloud-kc-realms-pepper-createbucket");
        dto.setBucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
        dto.stsRoleAccessBucketAssumeRoleWithWebIdentity("arn:aws:iam::1234:role/testing.katta.cloud-kc-realms-pepper-sts-chain-01");
        dto.stsRoleAccessBucketAssumeRoleTaggedSession("arn:aws:iam::1234:role/testing.katta.cloud-kc-realms-pepper-sts-chain-02");
        Mockito.verify(api, times(1)).apiStorageprofileS3stsPost(dto);
        Mockito.verify(api, times(1)).apiStorageprofileS3stsPost(any());
    }
}
