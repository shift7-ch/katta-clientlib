/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SessionFactory;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.shared.DefaultPathHomeFeature;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.api.UVFMasterkey;

import java.util.Base64;
import java.util.EnumSet;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.DeviceKeysServiceImpl;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.Bytes;
import com.nimbusds.jose.util.Base64URL;

public class HubVaultListService implements ListService {
    private static final Logger log = LogManager.getLogger(HubVaultListService.class);

    private final ProtocolFactory protocols;
    private final HubSession session;
    private final X509TrustManager trust;
    private final X509KeyManager key;
    private final VaultRegistry registry;
    private final HostPasswordStore keychain;

    public HubVaultListService(final ProtocolFactory protocols, final HubSession session,
                               final X509TrustManager trust, final X509KeyManager key, final VaultRegistry registry, final HostPasswordStore keychain) {
        this.protocols = protocols;
        this.session = session;
        this.trust = trust;
        this.key = key;
        this.registry = registry;
        this.keychain = keychain;
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        if(directory.isRoot()) {
            try {
                final ConfigDto configDto = new ConfigResourceApi(session.getClient()).apiConfigGet();
                log.debug("Read configuration {}", configDto);
                final AttributedList<Path> vaults = new AttributedList<>();
                for(final VaultDto vaultDto : new VaultResourceApi(session.getClient()).apiVaultsAccessibleGet(null)) {
                    if(Boolean.TRUE.equals(vaultDto.getArchived())) {
                        log.debug("Skip archived vault {}", vaultDto);
                        continue;
                    }
                    final DeviceKeys deviceKeys = new DeviceKeysServiceImpl().getDeviceKeys(session.getHost());
                    final UserKeys userKeys = new UserKeysServiceImpl(session).getUserKeys(session.getHost(), session.getMe(), deviceKeys);
                    log.debug("Read vault {}", vaultDto);
                    // Find storage configuration in vault metadata
                    final VaultServiceImpl vaultService = new VaultServiceImpl(session);
                    final UvfMetadataPayload vaultMetadata = vaultService.getVaultMetadataJWE(vaultDto.getId(), userKeys);
                    final Host bookmark = vaultService.getStorageBackend(protocols, configDto, vaultDto.getId(), vaultMetadata.storage());
                    final OAuthTokens tokens = keychain.findOAuthTokens(session.getHost());
                    log.debug("Use OAuth tokens {} from {}", tokens, session.getHost());
                    bookmark.getCredentials().withOauth(tokens);
                    log.debug("Configured {} for vault {}", bookmark, vaultDto);
                    final Session<?> storage = SessionFactory.create(bookmark, trust, key);
                    final String decryptedPayload = vaultMetadata.toJSON();
                    final HubCryptoVault vault = new HubCryptoVault(storage, new DefaultPathHomeFeature(bookmark).find(), vaultDto.getId(),
                            decryptedPayload);
                    registry.add(vault.load(session, new DisabledPasswordCallback() {
                        @Override
                        public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) {
                            final byte[] rawFileKey = Base64URL.from(vaultMetadata.seeds().get(vaultMetadata.latestSeed())).decode();
                            final byte[] rawNameKey = Base64URL.from(vaultMetadata.seeds().get(vaultMetadata.latestSeed())).decode();
                            final byte[] vaultKey = Bytes.concat(rawFileKey, rawNameKey);
                            return new VaultCredentials(Base64.getEncoder().encodeToString(vaultKey));
                        }
                    }));
                    final PathAttributes attr = storage.getFeature(AttributesFinder.class).find(vault.getHome());
                    try (UVFMasterkey masterKey = UVFMasterkey.fromDecryptedPayload(decryptedPayload)) {
                        attr.setDirectoryId(masterKey.rootDirId());
                    }
                    vaults.add(new Path(vault.getHome()).withType(EnumSet.of(Path.Type.volume, Path.Type.directory))
                            .withAttributes(attr));
                }
                return vaults;
            }
            catch(ApiException e) {
                throw new HubExceptionMappingService().map("Listing directory {0} failed", e, directory);
            }
            catch(SecurityFailure | AccessException | JsonProcessingException e) {
                throw new InteroperabilityException(e.getMessage());
            }
        }
        throw new NotfoundException(directory.getAbsolute());
    }

    @Override
    public void preflight(final Path directory) throws BackgroundException {
        if(directory.isRoot()) {
            return;
        }
        if(registry.contains(directory)) {
            return;
        }
        log.warn("Deny directory listing with no vault available for {}", directory);
        throw new AccessDeniedException();
    }
}
