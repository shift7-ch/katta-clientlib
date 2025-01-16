/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.shared.ThreadPoolSchedulerFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.DeviceResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.CachingUserKeysService;
import ch.iterate.hub.workflows.CachingWoTService;
import ch.iterate.hub.workflows.GrantAccessServiceImpl;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.WoTServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public class HubGrantAccessSchedulerService extends ThreadPoolSchedulerFeature<Host> {
    private static final Logger log = LogManager.getLogger(HubGrantAccessSchedulerService.class);

    private final HubSession session;

    public HubGrantAccessSchedulerService(final HubSession session) {
        super(Duration.ofSeconds(PreferencesFactory.get().getLong("hub.protocol.scheduler.period")).toMillis());
        this.session = session;
    }

    @Override
    protected Host operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session.getHost());
        try {
            new GrantAccessServiceImpl(new VaultResourceApi(session.getClient()), new UsersResourceApi(session.getClient()),
                    new CachingUserKeysService(new UserKeysServiceImpl(new VaultResourceApi(session.getClient()), new UsersResourceApi(session.getClient()), new DeviceResourceApi(session.getClient()))),
                    new CachingWoTService(new WoTServiceImpl(new UsersResourceApi(session.getClient())))).grantAccessToUsersRequiringAccessGrant(session.getHost(), FirstLoginDeviceSetupCallbackFactory.get());
        }
        catch(ApiException e) {
            log.error("Scheduler for {}: Automatic Access Grant failed.", session.getHost(), e);
            throw new HubExceptionMappingService().map(e);
        }
        catch(AccessException | SecurityFailure e) {
            log.error(String.format("Scheduler for %s: Automatic Access Grant failed.", session.getHost()), e);
            throw new BackgroundException(e);
        }
        return session.getHost();
    }
}
