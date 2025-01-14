/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public interface UserKeysService {
    /**
     * Get user key from hub and decrypt with device-keys
     */
    UserKeys getUserKeys(Host hub, FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure;

    /**
     * Get vault metadata JWE for vault:
     * 1. device key (EC) decrypts user key (EC)
     * 2. user key (EC) decrypts vault member key (AES)
     * 3. vault masterkey (AES) decrypts vault metadata (incl. key material (seeds))
     *
     * @param hub
     * @param vaultId Vault ID
     * @param prompt
     */
    UvfMetadataPayload getVaultMetadataJWE(Host hub, UUID vaultId, FirstLoginDeviceSetupCallback prompt) throws ApiException, SecurityFailure, AccessException;

    /**
     * Get vault access token containing vault member key and recovery key (if owner)
     * 1. device key (EC) decrypts user key (EC)
     * 2. user key (EC) decrypts vault member key (AES)
     *
     * @param hub
     * @param vaultId Vault ID
     * @param prompt
     */
    UvfAccessTokenPayload getVaultAccessTokenJWE(Host hub, UUID vaultId, FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure;
}
