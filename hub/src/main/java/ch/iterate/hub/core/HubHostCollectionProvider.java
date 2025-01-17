/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Controller;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostGroups;
import ch.cyberduck.core.exception.AccessDeniedException;

import ch.iterate.mountainduck.core.AbstractHostCollectionProvider;
import ch.iterate.mountainduck.fs.ConnectCallback;

public class HubHostCollectionProvider implements AbstractHostCollectionProvider {

    @Override
    public HubHostCollection defaultCollection() {
        return HubHostCollection.defaultCollection();
    }

    @Override
    public HostGroups defaultHostGroups() {
        return host -> HubHostCollection.defaultCollection().hostGroups(host);
    }

    @Override
    public String getTitleForGroupKey(String groupKey) {
        final Host hubBookmark = HubHostCollection.defaultCollection().lookup(groupKey);
        if(hubBookmark == null) {
            return groupKey;
        }
        return hubBookmark.getNickname();
    }

    @Override
    public <T extends Controller & ConnectCallback> void loadDefaultCollection(T c) throws AccessDeniedException {
        defaultCollection().load(c);
    }
}
