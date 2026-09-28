/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.shared.ThreadPoolSchedulerFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cloud.katta.client.ApiException;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.GrantAccessServiceImpl;

public class HubGrantAccessSchedulerService extends ThreadPoolSchedulerFeature<Host> {
    private static final Logger log = LogManager.getLogger(HubGrantAccessSchedulerService.class);

    private final HubSession session;

    public HubGrantAccessSchedulerService(final HubSession session) {
        super(HostPreferencesFactory.get(session.getHost()).getLong("hub.protocol.scheduler.period"));
        this.session = session;
    }

    @Override
    public Host operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session.getHost());
        try {
            final UserKeys userKeys = session.getUserKeys(DeviceSetupCallback.disabled);
            // Single request for all vaults this user can re-share, failures per vault are logged and skipped
            new GrantAccessServiceImpl(session).grantAccessToUsersRequiringAccessGrant(userKeys);
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
        return session.getHost();
    }
}
