/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.cryptomator.impl.uvf.JWKSetUVFVaultMetadataProvider;

import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JWEObjectJSON;

public class HubVaultMetadataUVFProvider extends JWKSetUVFVaultMetadataProvider {

    /**
     * Provider to create or load vault.
     *
     * @param keys          Member key and recovery key
     * @param vaultMetadata Encrypted metadata payload containing configuration and data related to the vault.
     */
    public HubVaultMetadataUVFProvider(final JWEObjectJSON vaultMetadata, final HubVaultKeys keys) throws SecurityFailure {
        super(vaultMetadata, keys.serialize());
    }
}
