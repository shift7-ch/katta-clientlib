/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Profile;
import ch.cyberduck.core.Protocol;

import cloud.katta.client.model.ConfigDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.serializer.HubConfigDtoDeserializer;
import cloud.katta.protocols.hub.serializer.StorageProfileDtoWrapperDeserializer;
import cloud.katta.protocols.hub.serializer.VaultDtoDeserializer;

public final class HubAwareProfile extends Profile {
    private final HubSession hub;

    public HubAwareProfile(final HubSession hub, final Protocol parent, final ConfigDto configDto, final StorageProfileDtoWrapper storageProfile, final VaultDto vaultDto) {
        super(parent, new HubConfigDtoDeserializer(configDto, new StorageProfileDtoWrapperDeserializer(storageProfile, new VaultDtoDeserializer(vaultDto))));
        this.hub = hub;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFeature(final Class<T> type) {
        if(type == HubSession.class) {
            return (T) hub;
        }
        return super.getFeature(type);
    }
}
