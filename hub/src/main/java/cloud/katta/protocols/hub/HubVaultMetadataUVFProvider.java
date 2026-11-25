/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.cryptomator.impl.uvf.JWKSetUVFVaultMetadataProvider;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.UnsupportedException;

import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.LockedVault;
import org.cryptomator.cryptolib.api.UvfLockedVault;
import org.cryptomator.cryptolib.api.UvfUnlockedVault;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

import cloud.katta.crypto.JWEPayload;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.jwk.JWKSet;

/**
 * Vault metadata of vaults managed by Hub. The storage configuration and the automatic access grant are custom fields
 * in the encrypted payload of vault.uvf.
 */
public class HubVaultMetadataUVFProvider extends JWKSetUVFVaultMetadataProvider {

    /**
     * Contents of vault.uvf from Hub. Null when creating a vault
     */
    @Nullable
    private final String metadata;

    /**
     * Payload with custom fields for a new vault. Null when loading a vault
     */
    @Nullable
    private final UVFMetadataPayload payload;

    /**
     * Provider to create a new vault.
     *
     * @param payload Storage configuration and automatic access grant
     * @param keys    Member key and recovery key with private key
     */
    public HubVaultMetadataUVFProvider(final UVFMetadataPayload payload, final HubVaultKeys keys) throws SecurityFailure {
        super(keys.serialize());
        this.metadata = null;
        this.payload = payload;
    }

    /**
     * Provider to load a vault.
     *
     * @param metadata Contents of vault.uvf from Hub
     * @param keys     Member key
     */
    public HubVaultMetadataUVFProvider(final String metadata, final HubVaultKeys keys) {
        this(metadata, new JWKSet(keys.memberKey()));
    }

    /**
     * Provider to load a vault.
     *
     * @param metadata Contents of vault.uvf from Hub
     * @param keys     Member key
     */
    public HubVaultMetadataUVFProvider(final String metadata, final JWKSet keys) {
        super(keys);
        this.metadata = metadata;
        this.payload = null;
    }

    @Override
    public UvfUnlockedVault create() throws BackgroundException {
        // TODO Write payload with storage configuration and automatic access grant as custom fields and
        //  org.cryptomator.hub.canonical to the protected header once supported by cryptolib (#614). Return the same
        //  vault on repeated calls, the metadata is uploaded to Hub before UVFVault.create calls this again.
        throw new UnsupportedException("Writing custom fields to vault metadata not supported");
    }

    /**
     * @return Contents of vault.uvf from Hub
     */
    @Nullable
    public String getMetadata() {
        return metadata;
    }

    /**
     * Decrypt the vault metadata and read the custom fields.
     *
     * @return Storage configuration and automatic access grant
     * @throws SecurityFailure Failure parsing or decrypting the vault metadata or reading custom fields
     */
    public UVFMetadataPayload getPayload() throws SecurityFailure {
        if(null == metadata) {
            throw new SecurityFailure("Missing vault metadata");
        }
        try {
            final LockedVault locked = LockedVault.parse(metadata.getBytes(StandardCharsets.US_ASCII));
            if(!(locked instanceof UvfLockedVault)) {
                throw new SecurityFailure("Unsupported vault metadata");
            }
            final UvfUnlockedVault unlocked = this.unlock((UvfLockedVault) locked);
            try {
                return JWEPayload.fromMap(unlocked.customFields(), UVFMetadataPayload.class);
            }
            finally {
                unlocked.destroy();
            }
        }
        catch(CryptoException e) {
            throw new SecurityFailure(e.getMessage(), e);
        }
        catch(BackgroundException e) {
            throw new SecurityFailure(e.getDetail(false), e);
        }
        catch(IllegalArgumentException e) {
            // Custom fields not matching payload
            throw new SecurityFailure(e.getMessage(), e);
        }
    }
}
