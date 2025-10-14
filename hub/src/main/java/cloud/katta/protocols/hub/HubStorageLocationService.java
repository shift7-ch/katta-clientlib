/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Location;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.model.StorageProfileDtoWrapper;

public class HubStorageLocationService implements Location {
    private static final Logger log = LogManager.getLogger(HubStorageLocationService.class);

    private final HubSession session;

    public HubStorageLocationService(final HubSession session) {
        this.session = session;
    }

    @Override
    public Name getDefault(final Path file) {
        return Location.unknown;
    }

    @Override
    public Set<Name> getLocations(final Path file) {
        try {
            final Set<Name> regions = new HashSet<>();
            final List<StorageProfileDto> storageProfileDtos = new StorageProfileResourceApi(session.getClient())
                    .apiStorageprofileGet(false);
            for(StorageProfileDto storageProfileDto : storageProfileDtos) {
                final StorageProfileDtoWrapper storageProfile = StorageProfileDtoWrapper.coerce(storageProfileDto);
                for(String region : storageProfile.getRegions()) {
                    regions.add(new StorageLocation(storageProfile.getId().toString(), region, storageProfile.getName()));
                }
            }
            return regions;
        }
        catch(ApiException e) {
            log.warn("Failed to retrieve storage locations from server", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Name getLocation(final Path file) {
        return StorageLocation.fromIdentifier(file.attributes().getRegion());
    }

    public static final class StorageLocation extends Name {
        private final String storageProfileId;
        /**
         * AWS location
         */
        private final String region;
        private final String storageProfileName;

        /**
         *
         * @param storageProfileId   UUID of storage profile configuration
         * @param region             AWS location
         * @param storageProfileName Description
         */
        public StorageLocation(final String storageProfileId, final String region, final String storageProfileName) {
            super(String.format("%s,%s", storageProfileId, null == region ? StringUtils.EMPTY : region));
            this.storageProfileId = storageProfileId;
            this.region = region;
            this.storageProfileName = storageProfileName;
        }

        /**
         *
         * @return Storage Profile Id
         */
        public String getProfile() {
            return storageProfileId;
        }

        public String getRegion() {
            return region;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", storageProfileName, region);
        }

        /**
         * Parse a storage location from an identifier containing storage profile and AWS location.
         *
         * @param identifier Storage profile identifier and AWS region separated by dash
         * @return Location with storage profile as identifier and AWS location as region
         */
        public static StorageLocation fromIdentifier(final String identifier) {
            final String[] parts = identifier.split(",");
            if(parts.length != 2) {
                return new StorageLocation(identifier, null, null);
            }
            return new StorageLocation(StringUtils.isBlank(parts[0]) ? null : parts[0], StringUtils.isBlank(parts[1]) ? null : parts[1], null);
        }
    }
}
