/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.testsetup.AbstractAdminCLIIT;
import cloud.katta.testsetup.CLIIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CLIIntegrationTest
class ArchiveStorageProfileIT extends AbstractAdminCLIIT {

    @Test
    public void testStorageProfileArchive() throws Exception {
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        final StorageProfileDto storageProfileDto = storageProfileResourceApi.apiStorageprofilePost(new StorageProfileDto(new StorageProfileS3StaticDto()
                .name("S3 static")
                .protocol(Protocol.S3_STATIC)
                .archived(false)
                .storageClass(S3StorageClass.STANDARD)
                .region("eu-west-1")
                .regions(Arrays.asList("eu-west-1"))
                .bucketPrefix("katta-test")
        ));
        {
            final Optional<StorageProfileS3StaticDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream().filter(p ->
                            StorageProfileDtoWrapper.coerce(p).getId().toString().equalsIgnoreCase(StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()))
                    .map(StorageProfileDto::getActualInstance).map(StorageProfileS3StaticDto.class::cast)
                    .findFirst();
            assertTrue(profile.isPresent());
            assertFalse(profile.get().getArchived());
        }
        final ArchiveStorageProfile cli = new ArchiveStorageProfile(null, null, null, accessToken, "http://localhost:8280", StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString());
        cli.call();
        {
            final Optional<StorageProfileS3StaticDto> profile = storageProfileResourceApi.apiStorageprofileGet(null).stream().filter(p ->
                    StorageProfileDtoWrapper.coerce(p).getId().toString().equalsIgnoreCase(StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString())).map(StorageProfileDto::getActualInstance).map(StorageProfileS3StaticDto.class::cast).findFirst();
            assertTrue(profile.isPresent());
            assertTrue(profile.get().getArchived());
        }
    }
}
