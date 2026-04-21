/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Acl;
import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.DirectoryDelimiterPathContainerService;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Share;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.AuthorityResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.AuthorityDto;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.UserDto;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;

public class HubVaultShareFeature implements Share<Void, Void> {

    private final HubSession session;

    public HubVaultShareFeature(final HubSession session) {
        this.session = session;
    }

    @Override
    public boolean isSupported(final Path file, final Type type) {
        switch(type) {
            case download:
                // Only when owner
                if(new DirectoryDelimiterPathContainerService().isContainer(file)) {
                    return file.attributes().getAcl().values().stream()
                            .anyMatch(roles -> roles.contains(new Acl.Role(Acl.Role.FULL)));
                }
        }
        return false;
    }

    @Override
    public Set<Sharee> getSharees(final Type type) throws BackgroundException {
        try {
            switch(type) {
                case download:
                    final Set<Sharee> users = new LinkedHashSet<>();
                    for(AuthorityDto authority : new AuthorityResourceApi(session.getClient()).apiAuthoritiesSearchGet("%", false)) {
                        final Object instance = authority.getActualInstanceRecursively();
                        if(instance instanceof UserDto) {
                            final UserDto user = (UserDto) instance;
                            users.add(new Sharee(user.getId(), String.format("%s (%s)", user.getName(), user.getEmail())));
                        }
                    }
                    return users;
            }
            return Collections.emptySet();
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    public DescriptiveUrl toDownloadUrl(final Path file, final Sharee sharee, final Void options, final PasswordCallback callback) throws BackgroundException {
        try {
            new VaultResourceApi(session.getClient()).apiVaultsVaultIdUsersUserIdPut(sharee.getIdentifier(),
                    UUID.fromString(file.attributes().getFileId()), Role.MEMBER);
            return DescriptiveUrl.EMPTY;
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    public DescriptiveUrl toUploadUrl(final Path file, final Sharee sharee, final Void options, final PasswordCallback callback) {
        return DescriptiveUrl.EMPTY;
    }
}
