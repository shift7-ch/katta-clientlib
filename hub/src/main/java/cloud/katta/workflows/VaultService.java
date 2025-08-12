/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.ProtocolFactory;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.model.ConfigDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfAccessTokenPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

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
     *
     * @param protocols Registered protocol implementations to access backend storage
     * @param hub       Hub API Connection
     * @param configDto Hub configuration
     * @param vaultId   Vault ID
     * @param metadata  Storage Backend configuration
     * @return Configuration
     * @throws AccessException Unsupported backend storage protocol
     * @throws ApiException    Server error accessing storage profile
     */
    Host getStorageBackend(final ProtocolFactory protocols, final HubSession hub, final ConfigDto configDto, UUID vaultId, VaultMetadataJWEBackendDto metadata, final OAuthTokens tokens) throws AccessException, ApiException;
}
