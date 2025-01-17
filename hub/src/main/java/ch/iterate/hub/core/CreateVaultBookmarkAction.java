/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.binding.SheetController;
import ch.cyberduck.core.Controller;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.exception.BackgroundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.core.callback.CreateVaultCallback;
import ch.iterate.hub.core.callback.CreateVaultModel;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.workflows.CreateVaultService;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.FirstLoginDeviceSetupException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.amazonaws.SdkBaseException;

import static ch.iterate.hub.core.HubHostCollection.defaultCollection;
import static ch.iterate.hub.protocols.hub.HubSession.createFromHubUrl;

/**
 * Create vault bookmark from the user input fetched in the CreateVaultModel.
 */
public class CreateVaultBookmarkAction {

    private static final Logger log = LogManager.getLogger(CreateVaultBookmarkAction.class.getName());
    private final Host hub;
    private final Controller controller;
    private final CreateVaultModel model;
    private final CreateVaultCallback cb;

    public CreateVaultBookmarkAction(final Host hub, final Controller controller, final CreateVaultModel model, final CreateVaultCallback cb) {
        this.hub = hub;
        this.controller = controller;
        this.model = model;
        this.cb = cb;
    }

    public SheetController run() throws BackgroundException {
        try {
            final String username = hub.getCredentials().getUsername();
            final HubSession hubSession = createFromHubUrl(new HostUrlProvider().withUsername(false).withPath(true).get(hub), username, controller);
            // sync both archived and non-archived profiles
            final List<StorageProfileDtoWrapper> storageProfiles = new StorageProfileResourceApi(hubSession.getClient()).apiStorageprofileGet(null).stream().map(StorageProfileDtoWrapper::coerce).collect(Collectors.toList());
            return new CreateVaultBookmarkController(storageProfiles, controller, model, m -> {
                try {
                    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 review @dko
                    new CreateVaultService(hubSession, controller).createVault(m);
                    defaultCollection().sync(hub.getUuid());
                }
                // N.B. CORS is not a problem when using Java SDK!
                catch(ApiException | SdkBaseException e) {
                    // retry
                    cb.create(new CreateVaultBookmarkAction(hub, controller, m.withReason(e.getMessage()), cb));
                }
                catch(AccessException | SecurityFailure | BackgroundException | FirstLoginDeviceSetupException e) {
                    // give up
                    log.error(e);
                }
            });
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    public interface Callback {
        void callback(final CreateVaultModel selected);
    }
}
