/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.features.Scheduler;
import ch.cyberduck.core.shared.ThreadPoolSchedulerFeature;

import java.util.concurrent.ExecutionException;

public class HubSchedulerService extends ThreadPoolSchedulerFeature<Void> {
    private final Scheduler<?>[] features;

    public HubSchedulerService(final long period, final Scheduler<?>... features) {
        super(period);
        this.features = features;
    }

    @Override
    protected Void operate(final PasswordCallback callback) throws BackgroundException {
        for(Scheduler<?> feature : features) {
            try {
                feature.execute(callback).get();
            }
            catch(InterruptedException e) {
                throw new ConnectionCanceledException(e);
            }
            catch(ExecutionException e) {
                if(e.getCause() instanceof BackgroundException) {
                    throw (BackgroundException) e.getCause();
                }
                throw new BackgroundException(e.getCause());
            }
        }
        return null;
    }

    @Override
    public void shutdown(final boolean gracefully) {
        for(Scheduler<?> feature : features) {
            feature.shutdown(gracefully);
        }
    }
}
