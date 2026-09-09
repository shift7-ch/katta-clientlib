/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.s3.S3Protocol;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import cloud.katta.client.model.ConfigDto;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.model.StorageProfileDtoWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HubStorageProfileTest {

    @Test
    public void testSTSEndpoint() throws Exception {
        final StorageProfileDtoWrapper storageProfile = Mockito.mock(StorageProfileDtoWrapper.class);
        Mockito.when(storageProfile.getStsEndpoint()).thenReturn("https://sts.minio");
        final HubStorageProfile profile = new HubStorageProfile(new S3Protocol(), Mockito.mock(ConfigDto.class), storageProfile);
        assertEquals("https://sts.minio", profile.getSTSEndpoint());
        Mockito.when(storageProfile.getStsEndpoint()).thenReturn(null);
        assertEquals("https://sts.amazonaws.com/", profile.getSTSEndpoint());
    }

    @Test
    public void testStorageClass() throws Exception {
        final StorageProfileDtoWrapper storageProfile = Mockito.mock(StorageProfileDtoWrapper.class);
        Mockito.when(storageProfile.getProtocol()).thenReturn(Protocol.S3_STATIC);
        Mockito.when(storageProfile.getStorageClass()).thenReturn(S3StorageClass.STANDARD_IA);
        final HubStorageProfile profile = new HubStorageProfile(new S3Protocol(), Mockito.mock(ConfigDto.class), storageProfile);
        // Read by ch.cyberduck.core.s3.S3StorageClassFeature
        final Map<String, String> properties = profile.getProperties();
        assertEquals("STANDARD_IA", properties.get("s3.storage.class"));
        assertEquals("STANDARD_IA", properties.get("s3.storage.class.options"));
    }

    @Test
    public void testPathStyleAccess() throws Exception {
        final StorageProfileDtoWrapper storageProfile = Mockito.mock(StorageProfileDtoWrapper.class);
        Mockito.when(storageProfile.getProtocol()).thenReturn(Protocol.S3_STATIC);
        Mockito.when(storageProfile.getPathStyleAccessEnabled()).thenReturn(true);
        final HubStorageProfile profile = new HubStorageProfile(new S3Protocol(), Mockito.mock(ConfigDto.class), storageProfile);
        assertEquals("true", profile.getProperties().get("s3.bucket.virtualhost.disable"));
    }
}
