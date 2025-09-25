/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3Dto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class StorageProfileAwsStaticSetupTest {
    @Test
    public void testCall() throws ApiException {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final UUID vaultId = UUID.randomUUID();
        final StorageProfileAwsStaticSetup cli = new StorageProfileAwsStaticSetup();
        cli.call(vaultId, api);

        final StorageProfileS3Dto dto = new StorageProfileS3Dto();
        dto.setId(vaultId);
        dto.setName("AWS S3 static");
        dto.setProtocol(Protocol.S3);
        dto.setArchived(false);
        dto.setScheme("https");
        dto.setPort(443);
        dto.setWithPathStyleAccessEnabled(false);
        dto.setStorageClass(S3STORAGECLASSES.STANDARD);
        Mockito.verify(api, times(1)).apiStorageprofileS3Put(dto);
        Mockito.verify(api, times(1)).apiStorageprofileS3Put(any());
    }
}
