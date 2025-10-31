/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;


import ch.cyberduck.core.PasswordStoreFactory;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.StorageProfileS3Dto;
import cloud.katta.testcontainers.AbtractAdminCliIT;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageProfileArchiveIT extends AbtractAdminCliIT {
    @Test
    public void testStorageProfileArchive() throws Exception {
        final String accessToken = PasswordStoreFactory.get().findOAuthTokens(hubSession.getHost()).getAccessToken();

        final String storageProfileId = "732D43FA-3716-46C4-B931-66EA5405EF1C".toLowerCase();
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(hubSession.getClient());
        {
            final Optional<StorageProfileS3Dto> profile = storageProfileResourceApi.apiStorageprofileS3Get().stream().filter(p -> p.getId().toString().toLowerCase().equals(storageProfileId)).findFirst();
            assertTrue(profile.isPresent());
            assertFalse(profile.get().getArchived());
        }
        new StorageProfileArchive().call("http://localhost:8280", accessToken, storageProfileId);
        {
            final Optional<StorageProfileS3Dto> profile = storageProfileResourceApi.apiStorageprofileS3Get().stream().filter(p -> p.getId().toString().toLowerCase().equals(storageProfileId)).findFirst();
            assertTrue(profile.isPresent());
            assertTrue(profile.get().getArchived());
        }
    }
}
