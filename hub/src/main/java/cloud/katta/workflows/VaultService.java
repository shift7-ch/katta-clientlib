/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JWEObjectJSON;

public interface VaultService {

    /**
     * Get the vault access token containing vault member key and recovery key (if owner)
     * 1. device key (EC) decrypts user key (EC)
     * 2. user key (EC) decrypts vault member key (AES)
     *
     * @param vaultId  Vault ID
     * @param userKeys EC key pair
     * @return User-specific access token
     * @throws ApiException    Error retrieving the vault key, encrypted for the current user, from the server
     * @throws SecurityFailure Decryption failure
     */
    UVFAccessTokenPayload getVaultAccessToken(UUID vaultId, UserKeys userKeys) throws ApiException, SecurityFailure;

    /**
     * Retrieve JSON Web Encryption (JWE) object containing the metadata of the vault.
     *
     * @param vaultId Vault ID
     * @return Encrypted vault.uvf JWE
     * @throws ApiException    If an error occurs while retrieving the vault metadata from the server.
     * @throws SecurityFailure If the returned JWE metadata cannot be parsed.
     */
    JWEObjectJSON getVaultMetadata(UUID vaultId) throws ApiException, SecurityFailure;
}
