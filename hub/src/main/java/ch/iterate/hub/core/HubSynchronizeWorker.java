/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProgressListener;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.profiles.ProfileDescription;
import ch.cyberduck.core.profiles.ProfilesFinder;
import ch.cyberduck.core.profiles.ProfilesSynchronizeWorker;
import ch.cyberduck.core.synchronization.ComparePathFilter;
import ch.cyberduck.core.synchronization.Comparison;
import ch.cyberduck.core.transfer.download.CompareFilter;
import ch.cyberduck.core.transfer.download.DownloadFilterOptions;
import ch.cyberduck.core.transfer.symlink.DisabledDownloadSymlinkResolver;
import ch.cyberduck.core.worker.Worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.VaultProfileBookmarkService;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.GrantAccessService;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.FirstLoginDeviceSetupException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import ch.iterate.mountainduck.fs.ConnectCallback;


public class HubSynchronizeWorker extends Worker<Void> {
    private static final Logger log = LogManager.getLogger(HubSynchronizeWorker.class);
    private final HubHostCollection collection;
    private final ConnectCallback connect;

    public HubSynchronizeWorker(final ConnectCallback connect, final HubHostCollection collection) {
        this.connect = connect;
        this.collection = collection;
    }

    @Override
    public Void run(final Session<?> session) throws BackgroundException {
        final HubSession hubSession = (HubSession) session;
        if(log.isInfoEnabled()) {
            log.info("/ START Hub Session Scheduler for {}.", hubSession);
        }
        try {
            // store hub bookmark with username for OAuth sharing with username (findOAuthTokens)
            // username is set in bookmark during HubSession.login()
            collection.collectionItemChanged(session.getHost());
            // 2024-01-30 decision dko+CE: keep bookmark sync non-generic for now, but re-use ProfilesSynchronizeWorker from core.
            syncStorageProfiles(hubSession);
            syncBookmarks(hubSession);
        }
        catch(BackgroundException e) {
            if(log.isErrorEnabled()) {
                log.error(String.format("\\ FAILED Hub Session Scheduler for %s: Syncing vaults failed.", hubSession), e);
            }
            throw e;
        }
        catch(ApiException e) {
            if(log.isErrorEnabled()) {
                log.error(String.format("\\ FAILED Hub Session Scheduler for %s: Syncing vaults failed.", hubSession), e);
            }
            throw new HubExceptionMappingService().map(e);
        }
        try {
            new GrantAccessService(hubSession).grantAccessToUsersRequiringAccessGrant();
        }
        catch(ApiException e) {
            if(log.isErrorEnabled()) {
                log.error(String.format("\\ FAILED Hub Session Scheduler for %s: Automatic Access Grant failed.", hubSession), e);
            }
            throw new HubExceptionMappingService().map(e);
        }
        catch(AccessException | SecurityFailure | FirstLoginDeviceSetupException e) {
            if(log.isErrorEnabled()) {
                log.error(String.format("\\ FAILED Hub Session Scheduler for %s: Automatic Access Grant failed.", hubSession), e);
            }
            throw new BackgroundException(e);
        }
        if(log.isInfoEnabled()) {
            log.info("\\ END Hub Session Scheduler for {}.", hubSession);
        }
        return null;
    }

    // public static for testing
    public static void syncStorageProfiles(final HubSession session) throws ApiException, BackgroundException {
        final Set<ProfileDescription> profileDescriptions = new ProfilesSynchronizeWorker(ProtocolFactory.get(), ProfilesFinder.Visitor.Noop) {
            @Override
            protected CompareFilter filter(final Session<?> session) {
                return new CompareFilter(new DisabledDownloadSymlinkResolver(), session,
                        new ComparePathFilter() {
                            @Override
                            public Comparison compare(final Path file, final Local local, final ProgressListener listener) {
                                // always download
                                return Comparison.remote;
                            }
                        }, new DownloadFilterOptions(session.getHost()));
            }
        }.run(session);
        for(final ProfileDescription profileDescription : profileDescriptions) {
            if(profileDescription.isInstalled()) {
                continue;
            }
            if(log.isInfoEnabled()) {
                log.info("Register {}", profileDescription);
            }
            ProtocolFactory.get().register(profileDescription.getFile().get());
        }
    }

    private void syncBookmarks(final HubSession session) throws BackgroundException {
        if(log.isInfoEnabled()) {
            log.info("Bookmark sync for {}...", session.getHost());
        }
        try {
            final List<VaultDto> vaults = session.getVaultApi().apiVaultsAccessibleGet(null);
            for(VaultDto vault : vaults) {
                try {
                    if(!vault.getArchived()) {
                        if(log.isInfoEnabled()) {
                            log.info("Adding bookmark for vault {} for hub {}", vault, session.getHost());
                        }
                        final Host bookmark = new VaultProfileBookmarkService(session).getVaultBookmark(vault.getId());
                        //  triggers menu update
                        collection.add(bookmark);
                        if(log.isInfoEnabled()) {
                            log.info("Added bookmark for vault {} for hub {}", vault, session.getHost());
                        }
                    }
                    else {
                        final Host bookmarkToRemove = collection.lookup(vault.getId().toString());
                        //  triggers menu update
                        final boolean removed = collection.remove(bookmarkToRemove);
                        if(removed) {
                            // disconnects fs
                            connect.disconnect(bookmarkToRemove);
                            if(log.isInfoEnabled()) {
                                log.info("Removed bookmark for vault {} for hub {}", vault, session.getHost());
                            }
                        }
                    }
                }
                catch(ApiException e) {
                    if(e.getCode() == 403) {
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Access not granted yet, ignoring vault %s (%s) for hub %s", vault.getName(), vault.getId(), session.getHost()), e);
                        }
                    }
                    else {
                        throw new HubExceptionMappingService().map(e);
                    }
                }
                catch(FirstLoginDeviceSetupException | AccessException | SecurityFailure e) {
                    throw new InteroperabilityException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
                }
            }
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
        if(log.isInfoEnabled()) {
            log.info(String.format("Bookmark sync done for %s...", session.getHost()));
        }
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        final HubSynchronizeWorker that = (HubSynchronizeWorker) o;

        return Objects.equals(collection, that.collection);
    }

    @Override
    public int hashCode() {
        return collection != null ? collection.hashCode() : 0;
    }
}
