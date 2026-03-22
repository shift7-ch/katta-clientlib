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
    private final UVFKeySet jwks;

    /**
     *
     * @param vaultId               the UUID representing the unique identifier of the vault.
     * @param jwks                  the key set used for encryption and securing the vault metadata.
     * @param vaultMetadata         the encrypted metadata payload containing configuration and data related to the vault.
     * @param rootDirectoryMetadata the metadata of the root directory, computed during initialization.
     * @param rootDirectoryIdHash   the hash representing the unique identifier of the root directory.
     */
    public HubVaultMetadataUVFProvider(final UUID vaultId,
                                       final UVFKeySet jwks,
                                       final byte[] vaultMetadata,
                                       final byte[] rootDirectoryMetadata,
                                       final String rootDirectoryIdHash) {
        super(vaultMetadata, rootDirectoryMetadata, rootDirectoryIdHash, new DefaultJWKCredentials(new JWKCredentials(jwks.memberKey())));
        this.vaultId = vaultId;
        this.jwks = jwks;
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
     * @return Retrieves the JWKS of the vault.
     */
    public UVFKeySet getKeys() {
        return jwks;
    }
}
