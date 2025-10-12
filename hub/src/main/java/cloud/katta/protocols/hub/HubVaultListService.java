/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.vault.VaultRegistry;
import ch.cyberduck.core.vault.VaultUnlockCancelException;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;
import java.util.EnumSet;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.VaultServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

public class HubVaultListService implements ListService {
    private static final Logger log = LogManager.getLogger(HubVaultListService.class);

    private final HubSession session;
    private final PasswordCallback prompt;

    public HubVaultListService(final HubSession session, final PasswordCallback prompt) {
        this.session = session;
        this.prompt = prompt;
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        try {
            final VaultRegistry registry = session.getRegistry();
            final AttributedList<Path> vaults = new AttributedList<>();
            for(final VaultDto vaultDto : new VaultResourceApi(session.getClient()).apiVaultsAccessibleGet(null)) {
                if(Boolean.TRUE.equals(vaultDto.getArchived())) {
                    log.debug("Skip archived vault {}", vaultDto);
                    continue;
                }
                log.debug("Load vault {}", vaultDto);
                try {
                    // Find storage configuration in vault metadata
                    final VaultServiceImpl vaultService = new VaultServiceImpl(session);
                    final UvfMetadataPayload vaultMetadata = vaultService.getVaultMetadataJWE(vaultDto.getId(), session.getUserKeys());
                    final HubUVFVault vault = new HubUVFVault(vaultDto.getId(),
                            new Path(vaultMetadata.storage().getDefaultPath(), EnumSet.of(Path.Type.directory, Path.Type.volume),
                                    new PathAttributes().setDisplayname(vaultMetadata.storage().getNickname())));
                    try {
                        registry.add(vault.load(session, prompt));
                        vaults.add(vault.getHome());
                        listener.chunk(directory, vaults);
                    }
                    catch(VaultUnlockCancelException e) {
                        log.warn("Skip vault {} with failure {} loading", vaultDto, e);
                        continue;
                    }
                }
                catch(ApiException e) {
                    if(HttpStatus.SC_FORBIDDEN == e.getCode()) {
                        log.warn("Skip vault {} with insufficient permissions {}", vaultDto, e);
                        continue;
                    }
                    throw e;
                }
                catch(AccessException | SecurityFailure e) {
                    throw new AccessDeniedException(e.getMessage(), e);
                }
            }
            return vaults;
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map("Listing directory {0} failed", e, directory);
        }
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
