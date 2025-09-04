/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfAccessTokenPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.model.StorageProfileDtoWrapper;
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
    UvfMetadataPayload getVaultMetadataJWE(UUID vaultId, UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;

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
     * Get storage configuration for vault
     *
     * @param metadataPayload Vault metadata including storage configuration
     * @return Storage profile
     */
    StorageProfileDtoWrapper getVaultStorageProfile(UvfMetadataPayload metadataPayload) throws ApiException, AccessException, SecurityFailure;
}
