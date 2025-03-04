/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import java.util.UUID;

import ch.cyberduck.core.ProtocolFactory;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEBackendDto;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public interface VaultService {

    /**
     * Get vault metadata JWE for vault:
     * 1. device key (EC) decrypts user key (EC)
     * 2. user key (EC) decrypts vault member key (AES)
     * 3. vault masterkey (AES) decrypts vault metadata (incl. key material (seeds))
     *
     * @param vaultId  Vault ID
     * @param userKeys EC key pair
     * @return Vault metadata
     */
    UvfMetadataPayload getVaultMetadataJWE(UUID vaultId, UserKeys userKeys) throws ApiException, SecurityFailure, AccessException;

    /**
     * Get vault access token containing vault member key and recovery key (if owner)
     * 1. device key (EC) decrypts user key (EC)
     * 2. user key (EC) decrypts vault member key (AES)
     *
     * @param vaultId  Vault ID
     * @param userKeys EC key pair
     * @return User specific access token
     */
    UvfAccessTokenPayload getVaultAccessTokenJWE(UUID vaultId, UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;

    /**
     * Prepares (virtual) bookmark for vault to access its configured storage backend.
     * @param protocols Registered protocol implementations to access backend storage
     * @param configDto Hub configuration
     * @param vaultId Vault ID
     * @param metadata Storage Backend configuration
     * @return Configuration
     * @throws AccessException Unsupported backend storage protocol
     * @throws ApiException Server error accessing storage profile
     */
    Host getStorageBackend(final ProtocolFactory protocols, final ConfigDto configDto, UUID vaultId, VaultMetadataJWEBackendDto metadata) throws AccessException, ApiException;
}
