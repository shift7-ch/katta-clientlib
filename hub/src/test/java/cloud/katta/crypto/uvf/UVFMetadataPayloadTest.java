/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.AlphanumericRandomStringService;

import org.cryptomator.cryptolib.api.UVFMasterkey;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UVFMetadataPayloadTest {

    @Test
    void testWorkaround() {
        // example of byte array -> UTF-8 -> byte array not working
        final byte[] rootDirId = Base64.getDecoder().decode("L3CoPPdXaaDgrM5YhBujn2t2LFTE5XjYUzC1htzk6tY=");
        assertFalse(Arrays.equals(rootDirId, new String(rootDirId, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
        // restricting to alphanumeric does work
        final byte[] rootDirId2 = new AlphanumericRandomStringService(4).random().getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(rootDirId2, new String(rootDirId2, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testUVFMasterkeyFromUvfMetadataPayload() throws Exception {
        final UVFMetadataPayload uvfMetadataPayload = UVFMetadataPayload.create();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        UVFMasterkey.fromDecryptedPayload(mapper.writeValueAsString(uvfMetadataPayload));
    }
}
