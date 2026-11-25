/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.TestProtocol;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultException;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.EnumSet;

import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFMetadataFixture;
import cloud.katta.crypto.uvf.UVFMetadataPayload;

import static org.junit.jupiter.api.Assertions.*;

class HubUVFVaultTest {

    private static final Path BUCKET = new Path("/bucket", EnumSet.of(Path.Type.directory, Path.Type.volume, Path.Type.vault));

    /**
     * Storage without vault.uvf
     */
    private static NullSession storage() {
        return new NullSession(new Host(new TestProtocol())) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T _getFeature(final Class<T> type) {
                if(type == Read.class) {
                    return (T) new Read() {
                        @Override
                        public InputStream read(final Path file, final TransferStatus status, final ConnectionCallback callback) throws NotfoundException {
                            throw new NotfoundException(file.getAbsolute());
                        }

                        @Override
                        public boolean offset(final Path file) {
                            return false;
                        }
                    };
                }
                return super._getFeature(type);
            }
        };
    }

    @Test
    void testLoadMetadataFromHub() throws Exception {
        final HubVaultKeys keys = HubVaultKeys.create();
        final String metadata = UVFMetadataFixture.create(keys, new UVFMetadataPayload());
        final NullSession storage = storage();
        final HubUVFVault vault = new HubUVFVault(storage, BUCKET, metadata);
        vault.load(storage, new HubVaultMetadataUVFProvider(metadata, keys));
        assertEquals(Vault.State.open, vault.getState());
        vault.close();
    }

    @Test
    void testLoadMissingMetadataFromHub() throws Exception {
        final HubVaultKeys keys = HubVaultKeys.create();
        final NullSession storage = storage();
        final HubUVFVault vault = new HubUVFVault(storage, BUCKET);
        assertThrows(VaultException.class, () -> vault.load(storage, new HubVaultMetadataUVFProvider(
                UVFMetadataFixture.create(keys, new UVFMetadataPayload()), keys)));
    }
}
