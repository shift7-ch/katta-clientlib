/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub.serializer;

import ch.cyberduck.core.serializer.Deserializer;

import java.util.Collections;
import java.util.List;

import cloud.katta.client.model.VaultDto;
import com.dd.plist.NSDictionary;

import static ch.cyberduck.core.Profile.VENDOR_KEY;

public class VaultDtoDeserializer extends ProxyDeserializer<NSDictionary> {

    private final VaultDto dto;

    public VaultDtoDeserializer(final VaultDto dto) {
        this(dto, ProxyDeserializer.empty());
    }

    /**
     * @param dto Storage configuration
     */
    public VaultDtoDeserializer(final VaultDto dto, final Deserializer<NSDictionary> parent) {
        super(parent);
        this.dto = dto;
    }

    @Override
    public String stringForKey(final String key) {
        // default profile and possible regions for UI:
        switch(key) {
            case VENDOR_KEY:
                // Allow lookup of profile with vault id
                return dto.getId().toString();
        }
        return super.stringForKey(key);
    }

    @Override
    public List<String> keys() {
        return Collections.singletonList(VENDOR_KEY);
    }
}
