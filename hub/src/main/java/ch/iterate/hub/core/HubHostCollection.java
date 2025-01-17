/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Controller;
import ch.cyberduck.core.Filter;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostFilter;
import ch.cyberduck.core.HostGroups;
import ch.cyberduck.core.HostReaderFactory;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalFactory;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.local.DefaultLocalDirectoryFeature;
import ch.cyberduck.core.preferences.SupportDirectoryFinderFactory;
import ch.cyberduck.core.profiles.ProfilesFinder;
import ch.cyberduck.core.serializer.Reader;
import ch.cyberduck.core.text.NaturalOrderCollator;
import ch.cyberduck.core.threading.BackgroundAction;
import ch.cyberduck.core.threading.BackgroundActionRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.iterate.mountainduck.fs.ConnectCallback;
import ch.iterate.mountainduck.fs.ConnectMode;

import static ch.iterate.hub.protocols.s3.CipherduckHostCustomProperties.HUB_UUID;


/**
 * Manage hub and vault bookmarks:
 * - keep OAuth to hubs alive all the time (triggering OAuth flow with user interaction if necessary - therefore holds reference to controller). (No synchronization if multiple hubs)
 * - sync vault bookmarks in hub session periodically in the background
 * - manage groups of vault bookmarks per hub for UI; the two-level hierarchy is not persisted in the bookmarks - it is restored during bookmark sync
 * - manage vault session restore
 * Decision 2024-01-16 dko,yla,ce: upon hub removal, it's OK not to remove stale hub and storage profiles.
 */
// N.B. we want to pass only one arg to load(), therefore use T extends A & B pattern
public class HubHostCollection<T extends Controller & ConnectCallback> extends BookmarkCollection {
    private static final Logger log = LogManager.getLogger(HubHostCollection.class);

    private final Map<String, Set<Host>> vaultBookmarks = new HashMap<>(); // hub UUID -> vault bookmarks
    private final Map<String, Host> hubs = new HashMap<>(); // hub UUID -> hub bookmark
    private final Map<String, PeriodicHubUpdater> updaters = new HashMap<>(); // hub UUID -> PeriodicHubUpdater

    protected static final Filter<Local> FILE_FILTER = new Filter<Local>() {
        @Override
        public boolean accept(final Local file) {
            return file.getName().endsWith(".duck");
        }

        @Override
        public Pattern toPattern() {
            return Pattern.compile(".*\\.duck");
        }
    };

    private static final HubHostCollection FAVORITES_COLLECTION = new HubHostCollection(
            LocalFactory.get(SupportDirectoryFinderFactory.get().find(), "Bookmarks")
    ) {
    };
    private T controller;

    private final Reader<Host> reader = HostReaderFactory.get();

    public HubHostCollection(final Local f) {
        super(f);
    }

    public HubHostCollection() {
        this(LocalFactory.get(SupportDirectoryFinderFactory.get().find(), "Bookmarks"));
    }

    /**
     * @return Singleton instance
     */
    public static HubHostCollection defaultCollection() {
        return FAVORITES_COLLECTION;
    }

    @Override
    public boolean add(Host item) {
        if(!ConnectMode.get(item).equals(ConnectMode.none)) {
            final String hubUUID = item.getProperty(HUB_UUID);
            if(!hubs.containsKey(hubUUID)) {
                return false;
            }
        }
        boolean b = super.add(item);
        if(b) {
            if(ConnectMode.get(item).equals(ConnectMode.none)) {
                if(!hubs.containsKey(item.getUuid())) {
                    hubs.putIfAbsent(item.getUuid(), item);
                    vaultBookmarks.put(item.getUuid(), new HashSet<>());
                }
                if(!updaters.containsKey(item.getUuid())) {
                    final PeriodicHubUpdater updater = new PeriodicHubUpdater(controller, controller, this, item);
                    updaters.put(item.getUuid(), updater);
                    updater.register();
                }
            }
            else {
                vaultBookmarks.get(item.getProperty(HUB_UUID)).add(item);
                try {
                    sync(item.getProperty(HUB_UUID));
                }
                catch(BackgroundException e) {
                    log.error("Hub sync failed after new vault was created", e);
                }
            }
        }
        return b;
    }

    public void sync(final String hubUuid) throws BackgroundException {
        log.debug("HubHostCollection.sync()");
        updaters.get(hubUuid).synchronize(ProfilesFinder.Visitor.Noop);
    }

    @Override
    public boolean remove(Object item) {
        boolean b = super.remove(item);
        if(b && item instanceof Host) {
            Host host = (Host) item;
            if(ConnectMode.get(host).equals(ConnectMode.none)) {
                if(hubs.containsKey(host.getUuid())) {
                    hubs.remove(host.getUuid());

                }

                final Set<Host> vaults = vaultBookmarks.get(host.getUuid());
                if(vaults != null) {
                    for(Host vault : vaults) {
                        this.remove(vault);
                        controller.disconnect(vault);
                    }
                }
                vaultBookmarks.remove(host.getUuid());

                final PeriodicHubUpdater updater = updaters.get(host.getUuid());
                if(updaters.containsKey(host.getUuid())) {
                    updater.unregister();
                    updaters.remove(host.getUuid());
                }
            }
            else {
                final String hubUUID = host.getProperty(HUB_UUID);
                vaultBookmarks.get(hubUUID).remove(item);
            }
        }
        return b;
    }

    public void load(T c) throws AccessDeniedException {
        if(log.isInfoEnabled()) {
            log.info(String.format("Reloading %s", folder.getAbsolute()));
        }
        this.controller = c;
        this.lock();
        try {
            if(!folder.exists()) {
                new DefaultLocalDirectoryFeature().mkdir(folder);
            }
            final AttributedList<Local> bookmarks = folder.list().filter(FILE_FILTER);
            final List<Host> vaults = new LinkedList<>();
            for(Local f : bookmarks) {
                try {
                    final Host bookmark = reader.read(f);
                    if(ConnectMode.get(bookmark).equals(ConnectMode.none)) {
                        this.add(bookmark);
                    }
                    else {
                        vaults.add(bookmark);
                    }
                }
                catch(AccessDeniedException e) {
                    log.error(String.format("Failure %s reading bookmark from %s", e, f));
                }
            }
            for(final Host vault : vaults) {
                final String hubUUID = vault.getProperty(HUB_UUID);
                if(hubs.containsKey(hubUUID)) {
                    this.add(vault);
                }
            }
        }
        finally {
            this.unlock();
        }
        // Sort using previously built index
        this.sort();
        // Mark collection as loaded and notify listeners.
        super.load();
    }

    @Override
    public Map<String, List<Host>> groups(HostGroups groups, HostFilter filter, Comparator<Host> comparator) {
        final Map<String, List<Host>> labels = new HashMap<>();
        for(Host hub : hubs.values()) {
            labels.put(hub.getUuid(), new ArrayList<>(vaultBookmarks.get(hub.getUuid())));
        }
        labels.forEach((s, hosts) -> hosts.sort(comparator));
        labels.entrySet().stream().sorted((o1, o2) -> new NaturalOrderCollator().compare(o1.getKey(), o2.getKey()));
        return labels;
    }

    public Set<String> hostGroups(Host host) {
        return vaultBookmarks.entrySet().stream().filter(entry -> entry.getValue().contains(host)).map(entry -> entry.getKey()).collect(Collectors.toSet());
    }

    @Override
    public void clear() {
        super.clear();
        hubs.clear();
        vaultBookmarks.clear();
        for(final PeriodicHubUpdater updater : updaters.values()) {
            updater.unregister();
        }
        updaters.clear();
    }

    @Deprecated // use for testing only
    public void cancel() throws InterruptedException {
        final BackgroundActionRegistry actions = controller.getRegistry();
        for(BackgroundAction action : actions.toArray(new BackgroundAction[actions.size()])) {
            log.info(String.format("Cancelling action %s", action));
            if((action != null)) {
                action.cancel();
            }
        }
    }
}
