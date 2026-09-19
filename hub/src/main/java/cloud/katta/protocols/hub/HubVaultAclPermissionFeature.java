/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.AclPermission;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jets3t.service.acl.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.AuthorityResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.AuthorityDto;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;

public class HubVaultAclPermissionFeature implements AclPermission {
    private static final Logger log = LogManager.getLogger(HubVaultAclPermissionFeature.class);

    private final HubSession session;

    public HubVaultAclPermissionFeature(final HubSession session) {
        this.session = session;
    }

    @Override
    public Acl getPermission(final Path file) throws BackgroundException {
        try {
            final List<MemberDto> members = new VaultResourceApi(session.getClient()).apiVaultsVaultIdMembersGet(UUID.fromString(file.attributes().getFileId()));
            log.debug("Retrieved {} members for vault {}", members.size(), file.attributes().getFileId());
            // Owner of vault
            return new Acl(new Acl.EmailUser(session.getMe().getEmail()), new Acl.Role(Acl.Role.FULL, false));
        }
        catch(ApiException e) {
            if(new HubExceptionMappingService().map(e) instanceof AccessDeniedException) {
                // Not owner but only member
                return new Acl(new Acl.EmailUser(session.getMe().getEmail()), new Acl.Role(Acl.Role.WRITE, false));
            }
            else {
                throw new HubExceptionMappingService().map("Failure to read attributes of {0}", e, file);
            }
        }
    }

    @Override
    public void setPermission(final Path file, final TransferStatus status) throws BackgroundException {
        throw new UnsupportedException();
    }

    @Override
    public List<Acl.User> getAvailableAclUsers(final List<Path> files) {
        try {
            final List<Acl.User> users = new ArrayList<>();
            for(AuthorityDto authority : new AuthorityResourceApi(session.getClient()).apiAuthoritiesSearchGet("%", false)) {
                final Object instance = authority.getActualInstanceRecursively();
                if(instance instanceof UserDto) {
                    final UserDto user = (UserDto) instance;
                    users.add(new Acl.EmailUser(user.getId(), user.getEmail(), false));
                }
            }
            return users;
        }
        catch(ApiException e) {
        }
    }

    @Override
    public List<Acl.Role> getAvailableAclRoles(final List<Path> files) {
        return Arrays.asList(
                new Acl.Role(Permission.PERMISSION_FULL_CONTROL.toString()),
                new Acl.Role(Permission.PERMISSION_WRITE.toString())
        );
    }
}
