/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Profile;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;

import cloud.katta.client.model.ConfigDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.serializer.HubConfigDtoDeserializer;
import cloud.katta.protocols.hub.serializer.StorageProfileDtoWrapperDeserializer;

public final class HubAwareProfile extends Profile {

    private final HubSession hub;
    private final OAuth2RequestInterceptor oauth;

    public HubAwareProfile(final HubSession hub, final OAuth2RequestInterceptor oauth, final Protocol parent, final ConfigDto configDto, final StorageProfileDtoWrapper storageProfile) {
        super(parent, new HubConfigDtoDeserializer(configDto, new StorageProfileDtoWrapperDeserializer(storageProfile)));
        this.hub = hub;
        this.oauth = oauth;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFeature(final Class<T> type) {
        if(type == HubSession.class) {
            return (T) hub;
        }
        if(type == OAuth2RequestInterceptor.class) {
            return (T) oauth;
        }
        return super.getFeature(type);
    }
}
