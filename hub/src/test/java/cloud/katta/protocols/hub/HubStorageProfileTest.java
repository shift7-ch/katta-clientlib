/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.s3.S3Protocol;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import cloud.katta.client.model.ConfigDto;
import cloud.katta.model.StorageProfileDtoWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HubStorageProfileTest {

    @Test
    public void testSTSEndpoint() throws Exception {
        final StorageProfileDtoWrapper storageProfile = Mockito.mock(StorageProfileDtoWrapper.class);
        Mockito.when(storageProfile.getStsEndpoint()).thenReturn("https://sts.minio");
        final HubStorageProfile profile = new HubStorageProfile(new S3Protocol(), Mockito.mock(ConfigDto.class), storageProfile);
        assertEquals("https://sts.minio", profile.getSTSEndpoint());
    }
}
