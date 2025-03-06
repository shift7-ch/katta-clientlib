/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.ConfigDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfAccessTokenPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.serializer.HubConfigDtoDeserializer;
import cloud.katta.protocols.hub.serializer.StorageProfileDtoWrapperDeserializer;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import static cloud.katta.crypto.uvf.UvfMetadataPayload.UniversalVaultFormatJWKS.memberKeyFromRawKey;
import static cloud.katta.protocols.s3.S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES;
import static cloud.katta.protocols.s3.S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN;

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
    public Host getStorageBackend(final ProtocolFactory protocols, final ConfigDto configDto, final UUID vaultId, final VaultMetadataJWEBackendDto vaultMetadata) throws ApiException, AccessException {
        if(null == protocols.forName(vaultMetadata.getProvider())) {
            log.debug("Load missing profile {}", vaultMetadata.getProvider());
            final StorageProfileDtoWrapper storageProfile = StorageProfileDtoWrapper.coerce(storageProfileResourceApi
                    .apiStorageprofileProfileIdGet(UUID.fromString(vaultMetadata.getProvider())));
            log.debug("Read storage profile {}", storageProfile);
            switch(storageProfile.getProtocol()) {
                case S3:
                case S3_STS:
                    final Profile profile = new Profile(protocols.forType(Protocol.Type.s3), new StorageProfileDtoWrapperDeserializer(
                            new HubConfigDtoDeserializer(configDto), storageProfile));
                    log.debug("Register storage profile {}", profile);
                    protocols.register(profile);
                    break;
                default:
                    throw new AccessException(String.format("Unsupported storage configuration %s", storageProfile.getProtocol().name()));
            }
        }
        final String provider = vaultMetadata.getProvider();
        log.debug("Lookup provider {} from vault metadata", provider);
        final Protocol protocol = protocols.forName(provider);
        final Host bookmark = new Host(protocol);
        log.debug("Configure bookmark for vault {}", vaultMetadata);
        bookmark.setNickname(vaultMetadata.getNickname());
        bookmark.setDefaultPath(vaultMetadata.getDefaultPath());
        if(vaultMetadata.getUsername() != null) {
            bookmark.getCredentials().setUsername(vaultMetadata.getUsername());
        }
        if(vaultMetadata.getPassword() != null) {
            bookmark.getCredentials().setPassword(vaultMetadata.getPassword());
        }
        if(protocol.getProperties().get(S3_ASSUMEROLE_ROLEARN) != null) {
            bookmark.setProperty(OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES, vaultId.toString());
        }
        // region as chosen by user upon vault creation (STS) or as retrieved from bucket (permanent)
        bookmark.setRegion(vaultMetadata.getRegion());
        return bookmark;
    }
}
