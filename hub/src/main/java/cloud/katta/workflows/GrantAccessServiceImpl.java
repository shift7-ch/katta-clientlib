/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.AuthorityResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.AuthorityDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubVaultMetadataUVFProvider;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

import static cloud.katta.crypto.KeyHelper.decodePublicKey;
import static java.util.Optional.ofNullable;

public class GrantAccessServiceImpl implements GrantAccessService {
    private static final Logger log = LogManager.getLogger(GrantAccessServiceImpl.class.getName());

    /**
     * Return the pending access grants known to the server right away instead of long-polling for new ones. Polling is
     * driven by {@link cloud.katta.protocols.hub.HubGrantAccessSchedulerService} instead.
     */
    private static final int NO_LONG_POLL = 0;

    private final VaultResourceApi vaultResourceApi;
    private final AuthorityResourceApi authorityResourceApi;
    private final VaultService vaultService;
    private final WoTService woTService;

    public GrantAccessServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()), new UsersResourceApi(hubSession.getClient()));
    }

    public GrantAccessServiceImpl(final VaultResourceApi vaultResourceApi, final UsersResourceApi usersResourceApi) {
        this(vaultResourceApi, new AuthorityResourceApi(usersResourceApi.getApiClient()),
                new VaultServiceImpl(vaultResourceApi), new WoTServiceImpl(usersResourceApi));
    }

    public GrantAccessServiceImpl(final VaultResourceApi vaultResourceApi, final AuthorityResourceApi authorityResourceApi,
                                  final VaultService vaultService, final WoTService woTService) {
        this.vaultResourceApi = vaultResourceApi;
        this.authorityResourceApi = authorityResourceApi;
        this.vaultService = vaultService;
        this.woTService = woTService;
    }

    @Override
    public void grantAccessToUsersRequiringAccessGrant(final UserKeys userKeys) throws ApiException {
        // Single request for all vaults the current user can re-share, regardless of role
        final Map<String, Set<String>> usersRequiringAccessGrant = vaultResourceApi.apiVaultsUsersRequiringAccessGrantGet(NO_LONG_POLL);
        log.info("{} vaults with users requiring access grant", usersRequiringAccessGrant.size());
        for(final Map.Entry<String, Set<String>> pending : usersRequiringAccessGrant.entrySet()) {
            final UUID vaultId = UUID.fromString(pending.getKey());
            try {
                this.grantAccessToUsersRequiringAccessGrant(vaultId, pending.getValue(), userKeys);
            }
            catch(ApiException | AccessException | SecurityFailure e) {
                log.warn("Grant access for vault {} failed with error {}", vaultId, e.getMessage());
                // Continue with next vault
            }
        }
    }

    @Override
    public void grantAccessToUsersRequiringAccessGrant(final UUID vaultId, final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        final Set<String> candidateUserIds = vaultResourceApi.apiVaultsUsersRequiringAccessGrantGet(NO_LONG_POLL).get(vaultId.toString());
        if(null == candidateUserIds || candidateUserIds.isEmpty()) {
            log.info("No users requiring access grant for vault {}", vaultId);
            return;
        }
        this.grantAccessToUsersRequiringAccessGrant(vaultId, candidateUserIds, userKeys);
    }

    @Override
    public void grantAccessToUsersRequiringAccessGrant(final UUID vaultId, final Set<String> candidateUserIds, final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        final UVFAccessTokenPayload accessToken = vaultService.getVaultAccessToken(vaultId, userKeys);
        try (final HubVaultMetadataUVFProvider vaultMetadataProvider = new HubVaultMetadataUVFProvider(vaultService.getVaultMetadata(vaultId),
                new HubVaultKeys(accessToken.key()))) {
            final UVFMetadataPayload vaultMetadata = vaultMetadataProvider.getPayload();
            if(vaultMetadata.automaticAccessGrant() == null || !ofNullable(vaultMetadata.automaticAccessGrant().getEnabled()).orElse(false)) {
                log.debug("Ignoring vault {} - automatic access grant disabled", vaultId);
                return;
            }
            log.info("{} users requiring access grant for vault {}", candidateUserIds.size(), vaultId);
            // 1. Resolve the candidates. This is the only source of their public keys: the Web of Trust below verifies the
            // signature chains against these very keys, so the server cannot have us encrypt for a key it substituted.
            final List<UserDto> candidates = authorityResourceApi.apiAuthoritiesGet(new ArrayList<>(candidateUserIds)).stream()
                    .map(AuthorityDto::getActualInstance)
                    .filter(authority -> authority instanceof UserDto)
                    .map(authority -> (UserDto) authority)
                    .filter(user -> user.getEcdhPublicKey() != null)
                    .collect(Collectors.toList());
            // 2. For users, who are considered trustworthy (i.e. the signature chain between the current user and the to-be-trusted user is shorter
            // than a configurable threshold), use the verified ECDH public key to encrypt the vault's member key:
            // trustThreshold: -1 means "grant to anyone", where 0, 1, 2 would be the number of edges between any vault owner and the grantee.
            final int trustThreshold = ofNullable(vaultMetadata.automaticAccessGrant().getTrustThreshold()).orElse(-1);
            final Map<String, Integer> verifiedTrustedUsers = trustThreshold < 0
                    ? Collections.<String, Integer>emptyMap()
                    : woTService.getTrustLevelsPerUserId(userKeys, candidates);
            final Map<String, String> accessTokens = new HashMap<>();
            for(final UserDto user : candidates) {
                if(trustThreshold >= 0) {
                    final Integer trustLevel = verifiedTrustedUsers.get(user.getId());
                    if(trustLevel == null) {
                        log.warn("Ignoring user {} for vault {} - no trust level", user.getId(), vaultId);
                        continue;
                    }
                    // trustLevel must be <= trustThreshold for automatic access grant
                    if(trustLevel > trustThreshold) {
                        log.warn("Ignoring user {} for vault {} - not verified (trust level {} > trustThreshold {})", user.getId(), vaultId, trustLevel, trustThreshold);
                        continue;
                    }
                }
                // else: -1 means grant to all
                // Member-level access only: the vault's recovery key is never shared through the automatic access grant flow
                accessTokens.put(user.getId(), accessToken.encryptForUser(decodePublicKey(user.getEcdhPublicKey()), false));
            }
            if(accessTokens.isEmpty()) {
                log.info("for vault {} - nothing to upload", vaultId);
                return;
            }
            // 3. Bulk-upload the collection of these JWEs to the server. (POST /vaults/${vaultId}/access-tokens/auto, {"user1": "jwe1", "user2": "jwe2", ...)
            vaultResourceApi.apiVaultsVaultIdAccessTokensAutoPost(vaultId, accessTokens);
            log.info("Uploaded JWE for users {} and vault {}", accessTokens.keySet(), vaultId);
        }
    }
}
