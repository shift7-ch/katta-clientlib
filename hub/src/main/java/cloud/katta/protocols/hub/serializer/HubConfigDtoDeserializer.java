/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub.serializer;

import ch.cyberduck.core.serializer.Deserializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cloud.katta.client.model.ConfigDto;
import com.dd.plist.NSDictionary;

import static ch.cyberduck.core.Profile.*;

public class HubConfigDtoDeserializer extends ProxyDeserializer<NSDictionary> {

    private final ConfigDto dto;

    public HubConfigDtoDeserializer(final ConfigDto dto) {
        this(dto, ProxyDeserializer.empty());
    }

    /**
     * @param dto Hub configuration
     */
    public HubConfigDtoDeserializer(final ConfigDto dto, final Deserializer<NSDictionary> parent) {
        super(parent);
        this.dto = dto;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <L> List<L> listForKey(final String key) {
        switch(key) {
            case PROPERTIES_KEY:
                final List<String> properties = new ArrayList<>(super.listForKey(key));
                return (List<L>) properties;
        }
        return super.listForKey(key);
    }

    @Override
    public String stringForKey(final String key) {
        switch(key) {
            case OAUTH_CLIENT_ID_KEY:
                // We use client_id="cryptomator"
                return dto.getKeycloakClientIdCryptomator();
            case OAUTH_AUTHORIZATION_URL_KEY:
                return dto.getKeycloakAuthEndpoint();
            case OAUTH_TOKEN_URL_KEY:
                return dto.getKeycloakTokenEndpoint();
        }
        return super.stringForKey(key);
    }

    @Override
    public List<String> keys() {
        return Arrays.asList(
                OAUTH_CLIENT_ID_KEY,
                OAUTH_AUTHORIZATION_URL_KEY,
                OAUTH_TOKEN_URL_KEY,
                PROPERTIES_KEY
        );
    }
}
