/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.exception.UnsupportedException;

import org.junit.jupiter.api.Test;

import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFMetadataFixture;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataAutomaticAccessGrantDto;
import cloud.katta.crypto.uvf.VaultMetadataStorageDto;
import cloud.katta.workflows.exceptions.SecurityFailure;

import static org.junit.jupiter.api.Assertions.*;

class HubVaultMetadataUVFProviderTest {

    private static UVFMetadataPayload payload() {
        return new UVFMetadataPayload()
                .withStorage(new VaultMetadataStorageDto()
                        .provider("provider")
                        .nickname("nickname")
                        .bucket("bucket"))
                .withAutomaticAccessGrant(new VaultMetadataAutomaticAccessGrantDto()
                        .enabled(true)
                        .trustThreshold(42));
    }

    @Test
    void testGetPayload() throws Exception {
        final HubVaultKeys keys = HubVaultKeys.create();
        final UVFMetadataPayload payload = payload();
        final String metadata = UVFMetadataFixture.create(keys, payload);
        // Loaded with member key only
        final HubVaultMetadataUVFProvider provider = new HubVaultMetadataUVFProvider(metadata, new HubVaultKeys(keys.memberKey(), null));
        assertSame(metadata, provider.getMetadata());
        final UVFMetadataPayload decrypted = provider.getPayload();
        assertEquals(payload.storage(), decrypted.storage());
        assertEquals(payload.automaticAccessGrant(), decrypted.automaticAccessGrant());
    }

    @Test
    void testGetPayloadWithoutCustomFields() throws Exception {
        final HubVaultKeys keys = HubVaultKeys.create();
        final UVFMetadataPayload decrypted = new HubVaultMetadataUVFProvider(UVFMetadataFixture.create(keys, new UVFMetadataPayload()), keys).getPayload();
        assertNull(decrypted.storage());
        assertNull(decrypted.automaticAccessGrant());
    }

    @Test
    void testGetPayloadWrongMemberKey() throws Exception {
        final String metadata = UVFMetadataFixture.create(HubVaultKeys.create(), payload());
        assertThrows(SecurityFailure.class, () -> new HubVaultMetadataUVFProvider(metadata, HubVaultKeys.create()).getPayload());
    }

    @Test
    void testGetPayloadMalformed() {
        assertThrows(SecurityFailure.class, () -> new HubVaultMetadataUVFProvider("{}", HubVaultKeys.create()).getPayload());
    }

    @Test
    void testCreateUnsupported() throws Exception {
        // Requires API in cryptolib to write custom fields
        assertThrows(UnsupportedException.class, () -> new HubVaultMetadataUVFProvider(payload(), HubVaultKeys.create()).create());
    }
}
