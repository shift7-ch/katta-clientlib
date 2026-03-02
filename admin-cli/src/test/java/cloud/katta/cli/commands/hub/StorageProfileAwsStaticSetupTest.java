/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3StaticDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class StorageProfileAwsStaticSetupTest {
    @Test
    public void testCall() throws ApiException {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final UUID vaultId = UUID.randomUUID();
        final StorageProfileAwsStaticSetup cli = new StorageProfileAwsStaticSetup();

        cli.region = "us-east-1";
        cli.regions = List.of();
        cli.bucketPrefix = "katta-";
        cli.call(vaultId, "AWS S3 static", api);


        final StorageProfileS3StaticDto dto = new StorageProfileS3StaticDto();
        dto.setId(vaultId);
        dto.setName("AWS S3 static");
        dto.setRegion("us-east-1");
        dto.setProtocol(Protocol.S3_STATIC);
        dto.setArchived(false);
        dto.setScheme("https");
        dto.setPort(443);
        dto.setWithPathStyleAccessEnabled(false);
        dto.setBucketPrefix("katta-");
        dto.setStorageClass(S3STORAGECLASSES.STANDARD);
        dto.setBucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
        dto.stsRoleCreateBucketClient("");
        dto.stsRoleCreateBucketHub("");
        Mockito.verify(api, times(1)).apiStorageprofileS3staticPost(dto);
        Mockito.verify(api, times(1)).apiStorageprofileS3staticPost(any());
    }
}
