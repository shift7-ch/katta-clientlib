/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

import static cloud.katta.crypto.KeyHelper.decodePublicKey;
import static java.util.Optional.ofNullable;

public class GrantAccessServiceImpl implements GrantAccessService {
    private static final Logger log = LogManager.getLogger(GrantAccessServiceImpl.class.getName());

    private final VaultResourceApi vaultResourceApi;
    private final VaultService vaultService;
    private final WoTService woTService;

    public GrantAccessServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()), new UsersResourceApi(hubSession.getClient()));
    }

    public GrantAccessServiceImpl(final VaultResourceApi vaultResourceApi, final UsersResourceApi usersResourceApi) {
        this(vaultResourceApi, new VaultServiceImpl(vaultResourceApi), new WoTServiceImpl(usersResourceApi));
    }

    public GrantAccessServiceImpl(final VaultResourceApi vaultResourceApi, final VaultService vaultService, final WoTService woTService) {
        this.vaultResourceApi = vaultResourceApi;
        this.vaultService = vaultService;
        this.woTService = woTService;
    }

    @Override
    public void grantAccessToUsersRequiringAccessGrant(final UUID vaultId, final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        final VaultDto vault = vaultResourceApi.apiVaultsVaultIdGet(vaultId);
        final UVFAccessTokenPayload accessToken = vaultService.getVaultAccessToken(vaultId, userKeys);
        final UVFMetadataPayload vaultMetadata = vaultService.decryptVaultMetadata(accessToken, vault.getUvfMetadataFile());
        if(vaultMetadata.automaticAccessGrant() == null || !ofNullable(vaultMetadata.automaticAccessGrant().getEnabled()).orElse(false)) {
            log.debug("Ignoring vault {} - automatic access grant disabled", vaultId);
            return;
        }
        // 1. Get Ids of trusted and verified users
        // maxWotDepth: -1 means "grant to anyone", where 0, 1, 2 would be the number of edges between any vault owner and the grantee.
        final int maxWotDepth = ofNullable(vaultMetadata.automaticAccessGrant().getMaxWotDepth()).orElse(-1);
        final Map<String, Integer> verifiedTrustedUsers = woTService.getTrustLevelsPerUserId(userKeys);
        // 2. For users, who are considered trustworthy (i.e. the signature chain between the current user and the to-be-trusted user is shorter
        // than a configurable threshold), use the verified ECDH public key to encrypt the vault's member key (and optionally its recovery key):
        final Map<String, String> accessTokens = new HashMap<>();
        final List<MemberDto> usersRequiringAccessGrant = vaultResourceApi.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId);
        log.info("Users requiring access grant for vault {}: {}", vaultId, usersRequiringAccessGrant);
        for(final MemberDto user : usersRequiringAccessGrant) {
            if(user.getEcdhPublicKey() == null) {
                log.debug("Ignoring user {} for vault {} - no user key yet", user, vaultId);
                continue;
            }
            if(maxWotDepth >= 0) {
                final Integer trustLevel = verifiedTrustedUsers.getOrDefault(user.getId(), -1);
                if(trustLevel == null) {
                    log.warn("Ignoring user {} for vault {} - not verified", user, vaultId);
                    continue;
                }
                // trustLevel must be <= maxWotDepth for automatic access grant
                if(trustLevel > maxWotDepth) {
                    log.warn("Ignoring user {} for vault {} - not verified", user, vaultId);
                    continue;
                }
            }
            accessTokens.put(user.getId(), accessToken.encryptForUser(decodePublicKey(user.getEcdhPublicKey())));
        }
        if(accessTokens.isEmpty()) {
            log.info("for vault {} - nothing to upload", vaultId);
            return;
        }
        // 3. Bulk-upload the collection of these JWEs to the server. (POST /vaults/${vaultId}/access-tokens, {"user1": "jwe1", "user2": "jwe2", ...)
        vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultId, accessTokens);
        log.info("Uploaded JWE for users {} and vault {}", accessTokens.keySet(), vaultId);
    }
}
