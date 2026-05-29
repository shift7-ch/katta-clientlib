/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultProvider;
import ch.cyberduck.core.vault.VaultRegistry;
import ch.cyberduck.core.vault.VaultUnlockCancelException;
import ch.cyberduck.core.vault.VaultVersion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;

public class HubVaultListService implements ListService {
    private static final Logger log = LogManager.getLogger(HubVaultListService.class);

    private final HubSession session;
    private final VaultProvider provider;

    public HubVaultListService(final HubSession session, final VaultProvider provider) {
        this.session = session;
        this.provider = provider;
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        if(directory.isRoot()) {
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
                        final Vault vault = provider.load(session,
                                new Path(directory, vaultDto.getId().toString(), EnumSet.of(Path.Type.directory, Path.Type.volume)),
                                new VaultVersion(VaultVersion.Type.UVF), new VaultCredentials());
                        log.info("Loaded vault {}", vault);
                        registry.add(vault);
                        final Path home = vault.getHome();
                        try {
                            final List<MemberDto> members = new VaultResourceApi(session.getClient()).apiVaultsVaultIdMembersGet(vaultDto.getId());
                            log.debug("Retrieved {} members for vault {}", members.size(), vaultDto.getId());
                            // Owner of vault
                            home.attributes().setAcl(new Acl(new Acl.EmailUser(session.getMe().getEmail()), new Acl.Role(Acl.Role.FULL, false)));
                        }
                        catch(ApiException e) {
                            if(new HubExceptionMappingService().map(e) instanceof AccessDeniedException) {
                                // Not owner but only member
                                home.attributes().setAcl(new Acl(new Acl.EmailUser(session.getMe().getEmail()), new Acl.Role(Acl.Role.WRITE, false)));
                            }
                            else {
                                throw e;
                            }
                        }
                        vaults.add(home);
                        listener.chunk(directory, vaults);
                    }
                    catch(VaultUnlockCancelException e) {
                        log.warn("Skip vault {} with failure {} loading", vaultDto, e);
                    }
                }
                return vaults;
            }
            catch(ApiException e) {
                throw new HubExceptionMappingService().map("Listing directory {0} failed", e, directory);
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
