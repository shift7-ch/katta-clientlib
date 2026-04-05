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

import java.util.List;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.VaultDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.GrantAccessServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

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
            final List<VaultDto> accessibleVaults = new VaultResourceApi(session.getClient()).apiVaultsAccessibleGet(Role.OWNER);
            final GrantAccessServiceImpl service = new GrantAccessServiceImpl(
                    new VaultResourceApi(session.getClient()),
                    new UsersResourceApi(session.getClient()));
            for(final VaultDto accessibleVault : accessibleVaults) {
                if(Boolean.TRUE.equals(accessibleVault.getArchived())) {
                    log.debug("Skip archived vault {}", accessibleVault);
                    continue;
                }
                try {
                    service.grantAccessToUsersRequiringAccessGrant(accessibleVault.getId(), userKeys);
                }
                catch(ApiException | AccessException | SecurityFailure e) {
                    log.warn("Grant access for vault {} failed with error {}", accessibleVault.getId(), e.getMessage());
                    // Continue with next vault
                }
            }
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
        return session.getHost();
    }
}
