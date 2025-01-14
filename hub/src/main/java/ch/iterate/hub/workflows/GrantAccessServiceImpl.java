/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.DeviceResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.MemberDto;
import ch.iterate.hub.client.model.Role;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static ch.iterate.hub.crypto.KeyHelper.decodePublicKey;

public class GrantAccessServiceImpl implements GrantAccessService {
    private static final Logger log = LogManager.getLogger(GrantAccessServiceImpl.class.getName());

    private final VaultResourceApi vaultResourceApi;
    private final UsersResourceApi usersResourceApi;

    private final UserKeysService userKeysService;
    private final WoTService woTService;

    public GrantAccessServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()), new UsersResourceApi(hubSession.getClient()), new DeviceResourceApi(hubSession.getClient()));
    }

    public GrantAccessServiceImpl(final VaultResourceApi vaultResourceApi, final UsersResourceApi usersResourceApi, final DeviceResourceApi deviceResourceApi) {
        this(vaultResourceApi, usersResourceApi, new UserKeysServiceImpl(vaultResourceApi, usersResourceApi, deviceResourceApi), new WoTServiceImpl(usersResourceApi));
    }

    public GrantAccessServiceImpl(final VaultResourceApi vaultResourceApi, final UsersResourceApi usersResourceApi,
                                  final UserKeysService userKeysService, final WoTService woTService) {
        this.vaultResourceApi = vaultResourceApi;
        this.usersResourceApi = usersResourceApi;
        this.userKeysService = userKeysService;
        this.woTService = woTService;
    }

    @Override
    public void grantAccessToUsersRequiringAccessGrant(final Host hub, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        final List<VaultDto> accessibleVaults = vaultResourceApi.apiVaultsAccessibleGet(Role.OWNER);
        final UserDto me = usersResourceApi.apiUsersMeGet(true);
        log.info("For hub {} ({})", usersResourceApi.getApiClient().getBasePath(), me);

        for(final VaultDto accessibleVault : accessibleVaults) {
            if(Boolean.TRUE.equals(accessibleVault.getArchived())) {
                continue;
            }
            this.grantAccessToUsersRequiringAccessGrant(hub, accessibleVault.getId(), prompt);
        }
    }

    protected void grantAccessToUsersRequiringAccessGrant(final Host hub, final UUID vaultId, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        final List<MemberDto> usersRequiringAccessGrant = vaultResourceApi.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId);
        log.info("Users requiring access grant for vault {}: {}", vaultId, usersRequiringAccessGrant);
        final UvfMetadataPayload vaultMetadata = userKeysService.getVaultMetadataJWE(hub, vaultId, prompt);
        final UvfAccessTokenPayload uvfAccessToken = userKeysService.getVaultAccessTokenJWE(hub, vaultId, prompt);
        if(vaultMetadata.automaticAccessGrant() == null || !Optional.ofNullable(vaultMetadata.automaticAccessGrant().getEnabled()).orElse(false)) {
            log.debug("Ignoring vault {} - automatic access grant disabled", vaultId);
            return;
        }
        // 1. Get Ids of trusted and verified users
        // maxWotDepth must be non-negative
        final int maxWotDepth = Optional.ofNullable(vaultMetadata.automaticAccessGrant().getMaxWotDepth()).orElse(-1);
        if(maxWotDepth < 0) {
            log.warn("Ignoring vault {} - invalid maxWotDepth value \"{}\"", vaultId, vaultMetadata.automaticAccessGrant().getMaxWotDepth());
            return;
        }
        final Map<String, Integer> verifiedTrustedUsers = woTService.getTrustLevelsPerUserId(userKeysService.getUserKeys(hub, prompt));
        // 2. For users, who are considered trustworthy (i.e. the signature chain between the current user and the to-be-trusted user is shorter than a configurable threshold), use the verified ECDH public key to encrypt the vault's member key (and optionally its recovery key):
        final Map<String, String> accessTokens = new HashMap<>();
        for(final MemberDto user : usersRequiringAccessGrant) {
            if(user.getEcdhPublicKey() == null) {
                log.debug("Ignoring user {} for vault {} - no user key yet", user, vaultId);
                continue;
            }
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
            final String userSpecificJWE;
            try {
                userSpecificJWE = uvfAccessToken.encryptForUser(decodePublicKey(user.getEcdhPublicKey()));
            }
            catch(JOSEException | JsonProcessingException | NoSuchAlgorithmException | InvalidKeySpecException | NotECKeyException e) {
                throw new SecurityFailure(e);
            }
            accessTokens.put(user.getId(), userSpecificJWE);
        }
        if(accessTokens.isEmpty()) {
            log.info("for vault {} - nothing to upload", vaultId);
            return;
        }
        // 3. Bulk-upload the collection of these JWEs to the server. (POST /vaults/${vaultId}/access-tokens, {"user1": "jwe1", "user2": "jwe2", ...)
        vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultId, accessTokens);
        if(log.isInfoEnabled()) {
            log.info("Uploaded JWE for users {} and vault {}", accessTokens.keySet(), vaultId);
        }
    }
}
