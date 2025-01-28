/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.Local;
import ch.cyberduck.core.NullFilter;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProgressListener;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.profiles.ProfileDescription;
import ch.cyberduck.core.profiles.ProfileMatcher;
import ch.cyberduck.core.profiles.ProfilesFinder;
import ch.cyberduck.core.profiles.ProfilesSynchronizer;
import ch.cyberduck.core.profiles.ProtocolFactoryProfilesSynchronizer;
import ch.cyberduck.core.profiles.RemoteProfilesFinder;
import ch.cyberduck.core.shared.OneTimeSchedulerFeature;
import ch.cyberduck.core.synchronization.ComparePathFilter;
import ch.cyberduck.core.synchronization.Comparison;
import ch.cyberduck.core.transfer.download.CompareFilter;
import ch.cyberduck.core.transfer.download.DownloadFilterOptions;
import ch.cyberduck.core.transfer.symlink.DisabledDownloadSymlinkResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;

public class HubStorageProfileSyncSchedulerService extends OneTimeSchedulerFeature<Set<ProfileDescription>> {
    private static final Logger log = LogManager.getLogger(HubStorageProfileSyncSchedulerService.class);

    private final ProtocolFactory registry = ProtocolFactory.get();
    private final HubSession session;

    public HubStorageProfileSyncSchedulerService(final HubSession session) {
        this.session = session;
    }

    @Override
    public Set<ProfileDescription> operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session);
        final ProfilesSynchronizer sync = new ProtocolFactoryProfilesSynchronizer(registry,
                new RemoteProfilesFinder(registry, session, new CompareFilter(new DisabledDownloadSymlinkResolver(), session,
                        new ComparePathFilter() {
                            @Override
                            public Comparison compare(final Path file, final Local local, final ProgressListener listener) {
                                // always download
                                return Comparison.remote;
                            }
                        }, new DownloadFilterOptions(session.getHost())), new NullFilter<>()));
        final Set<ProfileDescription> profiles = sync.sync(new ProfileMatcher() {
            @Override
            public Optional<ProfileDescription> compare(final Set<ProfileDescription> repository, final ProfileDescription installed) {
                return Optional.empty();
            }
        }, ProfilesFinder.Visitor.Prefetch);
        for(final ProfileDescription p : profiles) {
            if(p.isInstalled()) {
                continue;
            }
            if(log.isInfoEnabled()) {
                log.info("Register {}", p);
            }
            p.getFile().ifPresent(registry::register);
        }
        return profiles;
    }
}
