/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.CredentialsConfigurator;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.LoginCanceledException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfAccessTokenPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubAwareProfile;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.s3.S3AssumeRoleSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import static cloud.katta.crypto.uvf.UvfMetadataPayload.UniversalVaultFormatJWKS.memberKeyFromRawKey;

public class VaultServiceImpl implements VaultService {
    private static final Logger log = LogManager.getLogger(VaultServiceImpl.class);

    private final VaultResourceApi vaultResource;
    private final StorageProfileResourceApi storageProfileResourceApi;

    public VaultServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()), new StorageProfileResourceApi(hubSession.getClient()));
    }

    public VaultServiceImpl(final VaultResourceApi vaultResource, final StorageProfileResourceApi storageProfileResourceApi) {
        this.vaultResource = vaultResource;
        this.storageProfileResourceApi = storageProfileResourceApi;
    }

    @Override
    public UvfMetadataPayload getVaultMetadataJWE(final UUID vaultId, final UserKeys userKeys) throws ApiException, SecurityFailure, AccessException {
        final VaultDto vault = vaultResource.apiVaultsVaultIdGet(vaultId);
        // contains vault member key
        final UvfAccessTokenPayload accessToken = this.getVaultAccessTokenJWE(vaultId, userKeys);
        // extract and decode vault key
        final OctetSequenceKey rawMemberKey = memberKeyFromRawKey(Base64.getDecoder().decode(accessToken.key()));
        // decode vault metadata (incl. key material)
        try {
            return UvfMetadataPayload.decryptWithJWK(vault.getUvfMetadataFile(), rawMemberKey);
        }
        catch(ParseException | JsonProcessingException | JOSEException e) {
            throw new SecurityFailure(e);
        }
    }

    @Override
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final UUID vaultId, final UserKeys userKeys) throws ApiException, SecurityFailure {
        // Get the user-specific vault key with private user key
        final String userSpecificVaultJWE = vaultResource.apiVaultsVaultIdAccessTokenGet(vaultId, false);
        try {
            return userKeys.decryptAccessToken(userSpecificVaultJWE);
        }
        catch(ParseException | JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }

    @Override
    public StorageProfileDtoWrapper getVaultStorageProfile(final UvfMetadataPayload metadataPayload) throws ApiException {
        log.debug("Load profile {}", metadataPayload.storage().getProvider());
        return StorageProfileDtoWrapper.coerce(storageProfileResourceApi
                .apiStorageprofileProfileIdGet(UUID.fromString(metadataPayload.storage().getProvider())));
    }

    @Override
    public Session<?> getVaultStorageSession(final HubSession session, final UUID vaultId, final UvfMetadataPayload vaultMetadata) throws ApiException, AccessException {
        final StorageProfileDtoWrapper vaultStorageProfile = this.getVaultStorageProfile(vaultMetadata);
        switch(vaultStorageProfile.getProtocol()) {
            case S3_STATIC:
            case S3_STS:
                final VaultMetadataJWEBackendDto vaultStorageMetadata = vaultMetadata.storage();
                try {
                    final S3AssumeRoleSession storage = new S3AssumeRoleSession(session, vaultId, new Host(new HubAwareProfile(
                            ProtocolFactory.get().forType(ProtocolFactory.get().find(ProtocolFactory.BUNDLED_PROFILE_PREDICATE), Protocol.Type.s3), session.getConfig(), vaultStorageProfile),
                            session.getFeature(CredentialsConfigurator.class).reload().configure(session.getHost())
                                    .setUsername(vaultStorageMetadata.getUsername()).setPassword(vaultStorageMetadata.getPassword())).setRegion(vaultStorageMetadata.getRegion()));
                    log.debug("Configured {} for vault {}", storage, vaultId);
                    return storage;
                }
                catch(LoginCanceledException e) {
                    throw new AccessException(e);
                }
            default:
                log.warn("Unsupported storage configuration {} for vault {}", vaultStorageProfile.getProtocol(), vaultId);
                throw new AccessException(new InteroperabilityException());
        }
    }
}
