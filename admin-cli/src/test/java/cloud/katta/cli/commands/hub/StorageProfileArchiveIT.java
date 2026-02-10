/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import cloud.katta.cli.commands.CLIIntegrationTest;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.testcontainers.AbtractAdminCliIT;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CLIIntegrationTest
class StorageProfileArchiveIT extends AbtractAdminCliIT {
    @Test
    public void testStorageProfileArchive() throws Exception {
        final String storageProfileId = "732D43FA-3716-46C4-B931-66EA5405EF1C".toLowerCase();
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        {
            storageProfileResourceApi.apiStorageprofileS3staticPost(new StorageProfileS3StaticDto()
                    .id(UUID.fromString(storageProfileId))
                    .name("S3 static")
                    .protocol(Protocol.S3_STATIC)
                    .archived(false)
                    .storageClass(S3STORAGECLASSES.STANDARD)
                    .region("eu-west-1")
                    .regions(Arrays.asList("eu-west-1"))
                    .bucketPrefix("katta-test")
                    .stsRoleCreateBucketClient("")
                    .stsRoleCreateBucketHub("")
                    .bucketVersioning(true)
                    .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE)
            );

            final Optional<StorageProfileS3StaticDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream().filter(p -> StorageProfileDtoWrapper.coerce(p).getId().toString().toLowerCase().equals(storageProfileId))
                    .map(StorageProfileDto::getActualInstance).map(StorageProfileS3StaticDto.class::cast)
                    .findFirst();
            assertTrue(profile.isPresent());
            assertFalse(profile.get().getArchived());
        }
        new StorageProfileArchive().call("http://localhost:8280", accessToken, storageProfileId);
        {
            final Optional<StorageProfileS3StaticDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream().filter(p -> StorageProfileDtoWrapper.coerce(p).getId().toString().toLowerCase().equals(storageProfileId)).map(StorageProfileDto::getActualInstance).map(StorageProfileS3StaticDto.class::cast).findFirst();
            assertTrue(profile.isPresent());
            assertTrue(profile.get().getArchived());
        }
    }
}
