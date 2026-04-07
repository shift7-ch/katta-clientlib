/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile.s3;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import cloud.katta.client.JSON;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3StaticDto;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;

class S3StaticStorageProfileTest {

    @Test
    public void testCall() throws Exception {
        final StorageProfileResourceApi api = Mockito.mock(StorageProfileResourceApi.class);
        final UUID vaultId = UUID.randomUUID();
        final S3StaticStorageProfile cli = new S3StaticStorageProfile(vaultId.toString(), vaultId.toString(), "S3 static", "us-east-1", null,
                "https://s3.example.com", "katta-");
        cli.call(api);

        final StorageProfileS3StaticDto dto = new StorageProfileS3StaticDto();
        dto.setId(vaultId);
        dto.setName("S3 static");
        dto.setProtocol(Protocol.S3_STATIC);
        dto.setArchived(false);
        dto.setScheme("https");
        dto.setHostname("s3.example.com");
        dto.setPort(443);
        dto.setWithPathStyleAccessEnabled(true);
        dto.setStorageClass(S3STORAGECLASSES.STANDARD);
        dto.setRegion("us-east-1");
        dto.setRegions(List.of("us-east-1"));
        dto.setBucketPrefix("katta-");
        dto.setBucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
        dto.setBucketVersioning(false);
        dto.setBucketAcceleration(null);
        dto.stsRoleCreateBucketClient("");
        dto.stsRoleCreateBucketHub("");
        dto.stsEndpoint(null);
        Mockito.verify(api, times(1)).apiStorageprofileS3staticPost(dto);
        assertNotEquals("{}", new JSON().getMapper().writeValueAsString(dto));
    }
}
