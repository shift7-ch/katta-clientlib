/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;


import ch.cyberduck.core.CertificateIdentityCallbackFactory;
import ch.cyberduck.core.CertificateStoreFactory;
import ch.cyberduck.core.CertificateTrustCallbackFactory;
import ch.cyberduck.core.Controller;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallbackFactory;
import ch.cyberduck.core.LoginCallbackFactory;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.SessionPoolFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.pool.SessionPool;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.profiles.ProfilesFinder;
import ch.cyberduck.core.profiles.ProfilesUpdater;
import ch.cyberduck.core.ssl.DefaultTrustManagerHostnameCallback;
import ch.cyberduck.core.ssl.KeychainX509KeyManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.threading.WorkerBackgroundAction;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import ch.iterate.mountainduck.fs.ConnectCallback;

public class PeriodicHubUpdater implements ProfilesUpdater {
    private static final Logger log = LogManager.getLogger(PeriodicHubUpdater.class.getName());

    private final Controller controller;
    private final Duration delay;
    private final Timer timer = new Timer("profiles", true);
    private final HubHostCollection collection;
    private final Host hub;
    private final ConnectCallback connect;

    public PeriodicHubUpdater(final Controller controller, final ConnectCallback connect, final HubHostCollection collection, final Host hub) {
        this(controller, connect, collection, hub, Duration.ofSeconds(PreferencesFactory.get().getLong("hub.protocol.scheduler.period")));
    }

    public PeriodicHubUpdater(final Controller controller, final ConnectCallback connect, final HubHostCollection collection, final Host hub, final Duration delay) {
        this.controller = controller;
        this.connect = connect;
        this.delay = delay;
        this.collection = collection;
        this.hub = hub;
    }

    @Override
    public void unregister() {
        timer.cancel();
    }

    @Override
    public void register() {
        // TODO https://github.com/shift7-ch/cipherduck-hub/issues/4 improve log messages
        log.info(String.format("Register profiles checker hook after %s", delay));
        try {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if(log.isInfoEnabled()) {
                        log.info(String.format("Check for new profiles after %s", delay));
                    }
                    try {
                        synchronize(ProfilesFinder.Visitor.Noop);
                        if(log.isInfoEnabled()) {
                            log.info(String.format("Done check for new profiles after %s", delay));
                        }
                    }
                    catch(BackgroundException e) {
                        log.warn(String.format("Failure %s refreshing profiles", e));
                    }
                }
            }, 0L, delay.toMillis());
        }
        catch(IllegalStateException e) {
            log.warn(String.format("Failure scheduling timer. %s", e.getMessage()));
        }
    }

    // 2024-02-22: provisional decision yla+ce: use synchronized to avoid interference of event-triggered and scheduled syncs.
    // Having multiple syncs one after another is tolerable. In the future, we could introduce a queue/sempahore of size 1 which can be incremented by scheduler or events.
    public synchronized Future<Void> synchronize(final ProfilesFinder.Visitor visitor) throws BackgroundException {
        final SessionPool pool = SessionPoolFactory.create(
                new LoginConnectionService(LoginCallbackFactory.get(controller), HostKeyCallbackFactory.get(controller, hub.getProtocol()), PasswordStoreFactory.get(), controller),
                controller, hub,
                new KeychainX509TrustManager(CertificateTrustCallbackFactory.get(controller), new DefaultTrustManagerHostnameCallback(hub), CertificateStoreFactory.get()),
                new KeychainX509KeyManager(CertificateIdentityCallbackFactory.get(controller), hub, CertificateStoreFactory.get()),
                new VaultRegistry.DisabledVaultRegistry());
        return this.controller.background(new WorkerBackgroundAction<Void>(this.controller, pool, new HubSynchronizeWorker(connect, collection)));
    }
}

