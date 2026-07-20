/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.aws;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import cloud.katta.client.JSON;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;

class AWSStaticStorageProfileTest {

    @Test
    public void testCall() throws Exception {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final AWSStaticStorageProfile cli = new AWSStaticStorageProfile("http://localhost/myhub", "AWS S3 static", "us-east-1", null, "katta-");
        cli.call(api);

        final StorageProfileS3StaticDto dto = new StorageProfileS3StaticDto();
        dto.setName("AWS S3 static");
        dto.setRegion("us-east-1");
        dto.setRegions(List.of("us-east-1"));
        dto.setProtocol(Protocol.S3_STATIC);
        dto.setArchived(false);
        dto.pathStyleAccessEnabled(false);
        dto.setBucketPrefix("katta-");
        dto.setStorageClass(S3StorageClass.STANDARD);
        Mockito.verify(api, times(1)).apiStorageprofilePost(new StorageProfileDto(dto));
        assertNotEquals("{}", new JSON().getMapper().writeValueAsString(dto));
    }
}
