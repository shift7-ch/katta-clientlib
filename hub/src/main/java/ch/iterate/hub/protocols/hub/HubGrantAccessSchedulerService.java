/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.shared.OneTimeSchedulerFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.Role;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.DeviceKeysServiceImpl;
import ch.iterate.hub.workflows.GrantAccessServiceImpl;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public class HubGrantAccessSchedulerService extends OneTimeSchedulerFeature<Host> {
    private static final Logger log = LogManager.getLogger(HubGrantAccessSchedulerService.class);

    private final HubSession session;
    private final HostPasswordStore keychain;

    public HubGrantAccessSchedulerService(final HubSession session, final HostPasswordStore keychain) {
        this.session = session;
        this.keychain = keychain;
    }

    @Override
    public Host operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session.getHost());
        try {
            final UserKeys userKeys = new UserKeysServiceImpl(session).getUserKeys(session.getHost(), session.getMe(),
                    new DeviceKeysServiceImpl(keychain).getDeviceKeys(session.getHost()));
            final List<VaultDto> accessibleVaults = new VaultResourceApi(session.getClient()).apiVaultsAccessibleGet(Role.OWNER);
            final GrantAccessServiceImpl service = new GrantAccessServiceImpl(session);
            for(final VaultDto accessibleVault : accessibleVaults) {
                if(Boolean.TRUE.equals(accessibleVault.getArchived())) {
                    log.debug("Skip archived vault {}", accessibleVault);
                    continue;
                }
                service.grantAccessToUsersRequiringAccessGrant(accessibleVault.getId(), userKeys);
            }
            userKeys.destroy();
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
