/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Controller;
import ch.cyberduck.core.CredentialsConfiguratorFactory;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.ProfileWriterFactory;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.local.DefaultTemporaryFileService;
import ch.cyberduck.core.threading.AbstractBackgroundAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.protocols.hub.HubProfileBookmarkService;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;

import static ch.iterate.hub.protocols.hub.HubSession.getHubApiClientBootstrapping;

public class CreateHubBookmarkAction extends AbstractBackgroundAction<Host> {
    private static final Logger log = LogManager.getLogger(CreateHubBookmarkAction.class);
    private final String hubURL;
    private final HubHostCollection collection;

    private final Controller controller;

    public CreateHubBookmarkAction(final String hubURL, final HubHostCollection collection, final Controller controller) {
        // some reverse proxies do not handle double slashes well
        this.hubURL = hubURL.replaceAll("/$", "");
        this.collection = collection;
        this.controller = controller;
    }


    @Override
    public Host run() throws BackgroundException {
        try {
            final ConfigDto hubConfig = new ConfigResourceApi(getHubApiClientBootstrapping(hubURL, controller)).apiConfigGet();
            final String uuid = hubConfig.getUuid();

            final Profile hubProfile = new HubProfileBookmarkService().makeHubProfile(hubURL, hubConfig);
            final Local file = new DefaultTemporaryFileService().create(String.format("%s.cyberduckprofile", uuid));
            ProfileWriterFactory.get().write(hubProfile, file);
            ProtocolFactory.get().register(file);

            // hub bookmark
            final Host hubBookmark = new HubProfileBookmarkService().makeHubBookmark(hubProfile, hubURL, uuid);
            // required for testing with PasswordGrant (will be no-op in production/AuthorizationCode as HubProtocol does not implement CredentialsConfigurator)
            hubBookmark.withCredentials(CredentialsConfiguratorFactory.get(hubProfile).configure(hubBookmark));
            collection.add(hubBookmark);
            return hubBookmark;
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map(e);
        }
    }
}
