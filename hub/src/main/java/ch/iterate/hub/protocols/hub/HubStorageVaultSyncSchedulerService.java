/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.shared.OneTimeSchedulerFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public class HubStorageVaultSyncSchedulerService extends OneTimeSchedulerFeature<List<VaultDto>> {
    private static final Logger log = LogManager.getLogger(HubStorageVaultSyncSchedulerService.class);

    private final HubSession session;
    private final AbstractHostCollection bookmarks;

    public HubStorageVaultSyncSchedulerService(final HubSession session) {
        this(session, BookmarkCollection.defaultCollection());
    }

    public HubStorageVaultSyncSchedulerService(final HubSession session, final AbstractHostCollection bookmarks) {
        this.session = session;
        this.bookmarks = bookmarks;
    }

    @Override
    public List<VaultDto> operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session);
        final FirstLoginDeviceSetupCallback prompt = FirstLoginDeviceSetupCallbackFactory.get();
        log.info("Bookmark sync for {}", session.getHost());
        try {
            final List<VaultDto> vaults = new VaultResourceApi(session.getClient()).apiVaultsAccessibleGet(null);
            for(VaultDto vault : vaults) {
                try {
                    if(vault.getArchived()) {
                        final Host bookmarkToRemove = bookmarks.lookup(vault.getId().toString());
                        final boolean removed = bookmarks.remove(bookmarkToRemove);
                        if(removed) {
                            log.info("Removed bookmark for vault {} for hub {}", vault, session.getHost());
                        }
                    }
                    else {
                        log.info("Adding bookmark for vault {} for hub {}", vault, session.getHost());
                        final Host bookmark = new VaultProfileBookmarkService(session).getVaultBookmark(vault.getId(), prompt);
                        bookmarks.add(bookmark);
                        if(log.isInfoEnabled()) {
                            log.info("Added bookmark for vault {} for hub {}", vault, session.getHost());
                        }
                    }
                }
                catch(ApiException e) {
                    switch(e.getCode()) {
                        case 403:
                            log.info("Access not granted yet, ignoring vault {} ({}) for hub {}", vault.getName(), vault.getId(), session.getHost(), e);
                            break;
                        default:
                            throw new HubExceptionMappingService().map(e);
                    }
                }
                catch(AccessException | SecurityFailure e) {
                    throw new InteroperabilityException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
                }
            }
            return vaults;
        }
        catch(ApiException e) {
            log.error("Scheduler for {}: Syncing vaults failed.", session, e);
            throw new HubExceptionMappingService().map(e);
        }
    }
}
