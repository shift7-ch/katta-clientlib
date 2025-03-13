/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.shared.ThreadPoolSchedulerFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.DeviceResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.DeviceKeysServiceImpl;
import cloud.katta.workflows.GrantAccessService;
import cloud.katta.workflows.GrantAccessServiceImpl;
import cloud.katta.workflows.UserKeysServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

public class HubGrantAccessSchedulerService extends ThreadPoolSchedulerFeature<Host> {
    private static final Logger log = LogManager.getLogger(HubGrantAccessSchedulerService.class);

    private final HubSession session;
    private final HostPasswordStore keychain;
    private final VaultResourceApi vaults;
    private final UsersResourceApi users;
    private final DeviceResourceApi devices;
    private final GrantAccessService service;

    public HubGrantAccessSchedulerService(final HubSession session, final HostPasswordStore keychain) {
        this(session, keychain, new VaultResourceApi(session.getClient()), new UsersResourceApi(session.getClient()), new DeviceResourceApi(session.getClient()), new GrantAccessServiceImpl(session));
    }

    public HubGrantAccessSchedulerService(final HubSession session, final HostPasswordStore keychain, final VaultResourceApi vaults, final UsersResourceApi users, final DeviceResourceApi devices, final GrantAccessService service) {
        super(new HostPreferences(session.getHost()).getLong("hub.protocol.scheduler.period"));
        this.session = session;
        this.keychain = keychain;
        this.vaults = vaults;
        this.users = users;
        this.devices = devices;
        this.service = service;
    }

    @Override
    public Host operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session.getHost());
        try {
            final UserKeys userKeys = new UserKeysServiceImpl(users, devices).getUserKeys(session.getHost(), session.getMe(),
                    new DeviceKeysServiceImpl(keychain).getDeviceKeys(session.getHost()));

            final List<VaultDto> accessibleVaults = vaults.apiVaultsAccessibleGet(Role.OWNER);

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
