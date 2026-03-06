/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class ArchiveStorageProfileTest {

    @Test
    public void testCall() throws ApiException {
        final StorageProfileResourceApi proxyMock = Mockito.mock(StorageProfileResourceApi.class);
        final UUID vaultId = UUID.randomUUID();
        final ArchiveStorageProfile cli = new ArchiveStorageProfile(null, null, null, null, null, vaultId.toString());
        cli.call(proxyMock);
        Mockito.verify(proxyMock, times(1)).apiStorageprofileProfileIdPut(vaultId, true);
        Mockito.verify(proxyMock, times(1)).apiStorageprofileProfileIdPut(any(), any());
    }

}
