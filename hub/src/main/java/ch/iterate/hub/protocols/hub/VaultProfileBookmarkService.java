/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractProtocol;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.features.Location;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEBackendDto;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

import static ch.iterate.hub.protocols.s3.CipherduckHostCustomProperties.*;

public class VaultProfileBookmarkService {
    private static final Logger log = LogManager.getLogger(VaultProfileBookmarkService.class);

    private final HubSession hubSession;

    public VaultProfileBookmarkService(final HubSession hubSession) {
        this.hubSession = hubSession;
    }

    public static Protocol toProfileParentProtocol(final StorageProfileDtoWrapper wrappedProfileDto, final ConfigDto configDto) {
        final HashMap<String, String> properties = new HashMap<>();

        final boolean isSTS = wrappedProfileDto.getType().equals(StorageProfileS3STSDto.class);
        final String provider = wrappedProfileDto.getId().toString();
        final String description = String.format("Profile for storage config %s (%s)", wrappedProfileDto.getId(), wrappedProfileDto.getProtocol().toString());
        final String hostname = wrappedProfileDto.getHostname();
        final Scheme scheme = wrappedProfileDto.getScheme() != null ? Scheme.valueOf(wrappedProfileDto.getScheme()) : Scheme.https;
        final int defaultPort = wrappedProfileDto.getPort() != null ? wrappedProfileDto.getPort() : scheme.getPort();
        final String stsEndpoint = isSTS ? wrappedProfileDto.getStsEndpoint() : null;

        // default profile and possible regions for UI:
        final String region = isSTS ? wrappedProfileDto.getRegion() : null;
        final Set<Location.Name> regions = isSTS ? wrappedProfileDto.getRegions().stream().map(Location.Name::new).collect(Collectors.toSet()) : Collections.emptySet();
        if(wrappedProfileDto.getWithPathStyleAccessEnabled()) {
            properties.put("s3.bucket.virtualhost.disable", "true");
        }
        properties.put("3.storage.class.options", wrappedProfileDto.getStorageClass().toString());
        properties.put("3.storage.class", wrappedProfileDto.getStorageClass().toString());

        if(isSTS) {
            properties.put(S3_ASSUMEROLE_ROLEARN, wrappedProfileDto.getStsRoleArn());
            properties.put(OAUTH_TOKENEXCHANGE, "true");
            if(wrappedProfileDto.getStsRoleArn2() != null) {
                properties.put(S3_ASSUMEROLE_ROLEARN_2, wrappedProfileDto.getStsRoleArn2());
            }
            if(wrappedProfileDto.getStsDurationSeconds() != null) {
                properties.put(S3_ASSUMEROLE_DURATIONSECONDS, wrappedProfileDto.getStsDurationSeconds().toString());
            }
            if(configDto.getKeycloakClientIdCryptomatorVaults() != null) {
                properties.put(OAUTH_TOKENEXCHANGE_AUDIENCE, configDto.getKeycloakClientIdCryptomatorVaults());
            }
        }

        return new AbstractProtocol() {
            @Override
            public String getIdentifier() {
                // Decouple the client protocol identifier (s3-hub/s3-hub-sts) and the discriminator values (S3/S3STS) in the backend DB tables through openapi-generated client code.
                return isSTS ? "s3-hub-sts" : "s3-hub";
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String getProvider() {
                return provider;
            }

            @Override
            public String getDefaultHostname() {
                return hostname;
            }

            @Override
            public Scheme getScheme() {
                return scheme;
            }

            @Override
            public int getDefaultPort() {
                return defaultPort;
            }

            @Override
            public String getSTSEndpoint() {
                return stsEndpoint;
            }

            @Override
            public String getOAuthAuthorizationUrl() {
                return isSTS ? configDto.getKeycloakAuthEndpoint() : null;
            }

            @Override
            public String getOAuthTokenUrl() {
                return isSTS ? configDto.getKeycloakTokenEndpoint() : null;
            }

            @Override
            public String getOAuthClientId() {
                // We use client_id="cryptomator" in cipherduck, see discussion https://github.com/chenkins/cipherduck-hub/issues/6
                return isSTS ? configDto.getKeycloakClientIdCryptomator() : null;
            }

            @Override
            public String disk() {
                // do not serialize, inherit icon from storage protocol
                return null;
            }

            @Override
            public Map<String, String> getProperties() {
                return properties;
            }


            @Override
            public String getRegion() {
                return region;
            }

            @Override
            public Set<Location.Name> getRegions() {
                return regions;
            }

        };
    }

    public Host getVaultBookmark(final UUID vaultUUID, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        if(log.isInfoEnabled()) {
            log.info(String.format("Creating bookmark for vault %s for hub %s", vaultUUID, hubSession.getHost()));
        }

        final UserKeysServiceImpl userKeysService = new UserKeysServiceImpl(hubSession);
        final VaultServiceImpl vaultService = new VaultServiceImpl(hubSession);
        final UvfMetadataPayload jwe = vaultService.getVaultMetadataJWE(vaultUUID,
                userKeysService.getUserKeys(hubSession.getHost(), prompt));
        final VaultMetadataJWEBackendDto vaultMetadata = jwe.storage();
        final ConfigResourceApi vaultResourceApi = new ConfigResourceApi(hubSession.getClient());
        final String hubUUID = vaultResourceApi.apiConfigGet().getUuid();

        final Protocol parent = ProtocolFactory.get().forName(vaultMetadata.getProvider());
        if(null == parent) {
            throw new AccessException(String.format("No storage profile for %s", vaultMetadata.getProvider()));
        }
        final Host bookmark = new Host(parent);
        bookmark.setUuid(vaultUUID.toString());
        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 get nickname, default path etc. from metadata instead of user key
        bookmark.setNickname(vaultMetadata.getNickname());
        bookmark.setDefaultPath(vaultMetadata.getDefaultPath());
        if(vaultMetadata.getUsername() != null && vaultMetadata.getPassword() != null) {
            bookmark.withCredentials(new Credentials(vaultMetadata.getUsername(), vaultMetadata.getPassword()));
        }
        else {
            // set username for OAuth sharing with username (findOAuthTokens)
            bookmark.getCredentials().setUsername(hubSession.getHost().getCredentials().getUsername());
        }
        if(bookmark.getProperty(S3_ASSUMEROLE_ROLEARN) != null) {
            bookmark.setProperty(OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES, vaultUUID.toString());
        }
        // hubURL required for auto-loading of masterkey from hub
        bookmark.setProperty(HUB_URL, new HostUrlProvider().withUsername(false).withPath(true).get(hubSession.getHost()));
        // hubUsername required for auto-loading of masterkey, as we use the shared OAuth token from the keychain for hub authentication.
        // For permanent credentials, AccessKeyId in host credentials is different from hub username
        bookmark.setProperty(HUB_USERNAME, hubSession.getHost().getCredentials().getUsername());
        // hubUUID required for offline start
        bookmark.setProperty(HUB_UUID, hubUUID);
        // region as chosen by user upon vault creation (STS) or as retrieved from bucket (permanent)
        bookmark.setRegion(vaultMetadata.getRegion());
        if(log.isInfoEnabled()) {
            log.info(String.format("Created vault bookmark %s for hub %s", bookmark, hubSession.getHost()));
        }
        return bookmark;
    }
}
