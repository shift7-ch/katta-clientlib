/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.shared.OneTimeSchedulerFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEBackendDto;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

import static ch.iterate.hub.protocols.hub.HubSession.HUB_UUID;
import static ch.iterate.hub.protocols.s3.S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES;
import static ch.iterate.hub.protocols.s3.S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN;

public class HubStorageVaultSyncSchedulerService extends OneTimeSchedulerFeature<List<VaultDto>> {
    private static final Logger log = LogManager.getLogger(HubStorageVaultSyncSchedulerService.class);

    private final HubSession session;
    private final AbstractHostCollection bookmarks;
    private final HostPasswordStore keychain;

    public HubStorageVaultSyncSchedulerService(final HubSession session) {
        this(session, BookmarkCollection.defaultCollection());
    }

    public HubStorageVaultSyncSchedulerService(final HubSession session, final AbstractHostCollection bookmarks) {
        this(session, bookmarks, PasswordStoreFactory.get());
    }

    public HubStorageVaultSyncSchedulerService(final HubSession session, final HostPasswordStore keychain) {
        this(session, BookmarkCollection.defaultCollection(), keychain);
    }

    public HubStorageVaultSyncSchedulerService(final HubSession session, final AbstractHostCollection bookmarks, final HostPasswordStore keychain) {
        this.session = session;
        this.bookmarks = bookmarks;
        this.keychain = keychain;
    }

    @Override
    public List<VaultDto> operate(final PasswordCallback callback) throws BackgroundException {
        log.info("Scheduler for {}", session);
        final FirstLoginDeviceSetupCallback prompt = FirstLoginDeviceSetupCallbackFactory.get();
        log.info("Bookmark sync for {}", session.getHost());
        try {
            final UserKeys userKeys = new UserKeysServiceImpl(session).getUserKeys(session.getHost(), prompt);
            final List<VaultDto> vaults = new VaultResourceApi(session.getClient()).apiVaultsAccessibleGet(null);
            for(final VaultDto vaultDto : vaults) {
                try {
                    final UUID vaultId = vaultDto.getId();
                    if(Boolean.TRUE.equals(vaultDto.getArchived())) {
                        log.debug("Lookup bookmark for archived vault {}", vaultDto);
                        final Host existing = bookmarks.lookup(vaultId.toString());
                        if(existing != null) {
                            log.warn("Delete bookmark {} for archived vault {}", existing, vaultDto);
                            final boolean removed = bookmarks.remove(existing);
                            if(removed) {
                                log.info("Removed bookmark for vault {} for hub {}", vaultDto, session.getHost());
                            }
                        }
                    }
                    else {
                        log.info("Adding bookmark for vault {} in hub {}", vaultDto, session.getHost());
                        // Find storage configuration in vault metadata
                        final VaultMetadataJWEBackendDto vaultMetadata = new VaultServiceImpl(session).getVaultMetadataJWE(vaultId, userKeys).storage();
                        final Host bookmark = toBookmark(session.getHost(), vaultId, vaultMetadata);
                        if(bookmark.getCredentials().isPasswordAuthentication()) {
                            log.warn("Save static access tokens for {} in keychain", vaultDto);
                            keychain.save(bookmark);
                            bookmark.getCredentials().reset();
                        }
                        bookmarks.add(bookmark);
                        log.info("Added bookmark {} for vault {} for hub {}", bookmarks, vaultDto, session.getHost());
                    }
                }
                catch(AccessDeniedException e) {
                    log.info("Access not granted yet, ignoring vault {} ({}) for hub {}", vaultDto.getName(), vaultDto.getId(), session.getHost(), e);
                }
            }
            return vaults;
        }
        catch(ApiException e) {
            log.error("Scheduler for {}: Syncing vaults failed.", session, e);
            throw new HubExceptionMappingService().map(e);
        }
        catch(AccessException | SecurityFailure e) {
            throw new InteroperabilityException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
        }
    }

    public static Host toBookmark(final Host hub, final UUID vaultId, final VaultMetadataJWEBackendDto vaultMetadata) throws AccessException {
        final String provider = vaultMetadata.getProvider();
        log.debug("Lookup provider {} from vault metadata", provider);
        final Protocol protocol = ProtocolFactory.get().forName(provider);
        if(null == protocol) {
            throw new AccessException(String.format("No storage profile for %s", provider));
        }
        final Host bookmark = new Host(protocol);
        log.debug("Configure bookmark with vault id {}", vaultId.toString());
        bookmark.setUuid(vaultId.toString());
        bookmark.setNickname(vaultMetadata.getNickname());
        bookmark.setDefaultPath(vaultMetadata.getDefaultPath());
        if(vaultMetadata.getUsername() != null && vaultMetadata.getPassword() != null) {
            final Credentials credentials = new Credentials(vaultMetadata.getUsername(), vaultMetadata.getPassword());
            bookmark.setCredentials(credentials);
        }
        else {
            log.debug("Use OAuth credentials from {}", hub);
            final HostPasswordStore keychain = PasswordStoreFactory.get();
            bookmark.setCredentials(new Credentials().withOauth(keychain.findOAuthTokens(hub)));
        }
        if(bookmark.getProperty(S3_ASSUMEROLE_ROLEARN) != null) {
            bookmark.setProperty(OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES, vaultId.toString());
        }
        log.debug("Reference {} in bookmark {}", hub.getUuid(), bookmark);
        bookmark.setProperty(HUB_UUID, hub.getUuid());
        // region as chosen by user upon vault creation (STS) or as retrieved from bucket (permanent)
        bookmark.setRegion(vaultMetadata.getRegion());
        return bookmark;
    }
}
