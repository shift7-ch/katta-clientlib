/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.vault.DefaultJWKCredentials;
import ch.cyberduck.core.vault.DefaultVaultMetadataUVFProvider;
import ch.cyberduck.core.vault.JWKCredentials;
import ch.cyberduck.core.vault.VaultMetadataProvider;

import java.util.UUID;

public class HubVaultMetadataUVFProvider extends DefaultVaultMetadataUVFProvider {

    private final UUID vaultId;
    private final HubVaultKeys keys;

    /**
     * Provider to create or load vault.
     *
     * @param vaultId               UUID representing the unique identifier of the vault.
     * @param keys                  Member key and recovery key
     * @param vaultMetadata         Encrypted metadata payload containing configuration and data related to the vault.
     * @param rootDirectoryMetadata Metadata of the root directory
     * @param rootDirectoryIdHash   Hash representing the unique identifier of the root directory.
     */
    public HubVaultMetadataUVFProvider(final UUID vaultId,
                                       final HubVaultKeys keys,
                                       final byte[] vaultMetadata,
                                       final byte[] rootDirectoryMetadata,
                                       final String rootDirectoryIdHash) {
        super(vaultMetadata, rootDirectoryMetadata, rootDirectoryIdHash, new DefaultJWKCredentials(new JWKCredentials(keys.memberKey())));
        this.vaultId = vaultId;
        this.keys = keys;
    }

    public static HubVaultMetadataUVFProvider cast(VaultMetadataProvider provider) throws ConnectionCanceledException {
        if(provider instanceof HubVaultMetadataUVFProvider) {
            return (HubVaultMetadataUVFProvider) provider;
        }
        else {
            throw new ConnectionCanceledException(new UnsupportedException(provider.getClass().getName()));
        }
    }

    /**
     * Retrieves the unique identifier of the vault.
     *
     * @return the UUID representing the vault's identifier.
     */
    public UUID getVaultId() {
        return vaultId;
    }

    /**
     *
     * @return Member key for the vault
     */
    public HubVaultKeys getKeys() {
        return keys;
    }
}
