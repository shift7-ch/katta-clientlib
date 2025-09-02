/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LocaleFactory;
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
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.api.UVFMasterkey;

import java.text.MessageFormat;
import java.util.EnumSet;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.ConfigResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.ConfigDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayloadPasswordCallback;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.DeviceKeysServiceImpl;
import cloud.katta.workflows.UserKeysServiceImpl;
import cloud.katta.workflows.VaultServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;

public class HubVaultListService implements ListService {
    private static final Logger log = LogManager.getLogger(HubVaultListService.class);

    private final HubSession session;
    private final OAuthTokens tokens;

    public HubVaultListService(final HubSession session, final OAuthTokens tokens) {
        this.session = session;
        this.tokens = tokens;
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
                    final UvfMetadataPayload vaultMetadata;
                    try {
                        vaultMetadata = vaultService.getVaultMetadataJWE(vaultDto.getId(), userKeys);
                    }
                    catch(ApiException e) {
                        if(HttpStatus.SC_FORBIDDEN == e.getCode()) {
                            log.warn("Skip vault {} with insufficient permissions", vaultDto);
                            continue;
                        }
                        throw e;
                    }
                    final Host bookmark = vaultService.getStorageBackend(session, configDto, vaultDto.getId(),
                            vaultMetadata.storage(), tokens);
                    log.debug("Configured {} for vault {}", bookmark, vaultDto);
                    final Session<?> storage = SessionFactory.create(bookmark,
                            session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class));
                    final HubUVFVault vault = new HubUVFVault(storage, new DefaultPathHomeFeature(bookmark).find());
                    final VaultRegistry registry = session.getRegistry();
                    registry.add(vault.load(session, new UvfMetadataPayloadPasswordCallback(vaultMetadata)));
                    final PathAttributes attr = storage.getFeature(AttributesFinder.class).find(vault.getHome());
                    try (UVFMasterkey masterKey = UVFMasterkey.fromDecryptedPayload(vaultMetadata.toJSON())) {
                        attr.setDirectoryId(masterKey.rootDirId());
                    }
                    attr.setDisplayname(vaultMetadata.storage().getNickname());
                    vaults.add(new Path(vault.getHome()).withType(EnumSet.of(Path.Type.volume, Path.Type.directory))
                            .withAttributes(attr));
                    listener.chunk(directory, vaults);
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
        if(session.getRegistry().contains(directory)) {
            return;
        }
        log.warn("Deny directory listing with no vault available for {}", directory);
        throw new AccessDeniedException(MessageFormat.format(LocaleFactory.localizedString("Listing directory {0} failed", "Error"),
                directory.getName())).withFile(directory);
    }
}
