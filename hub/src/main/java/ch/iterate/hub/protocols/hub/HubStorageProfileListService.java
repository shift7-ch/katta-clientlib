
/*
 * Copyright (c) 2025 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.AttributesAdapter;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.serializer.impl.dd.PlistSerializer;
import ch.cyberduck.core.transfer.TransferStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.model.StorageProfileDtoWrapperException;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;

import static ch.iterate.hub.protocols.hub.VaultProfileBookmarkService.toProfileParentProtocol;

public class HubStorageProfileListService implements ListService, Read, AttributesAdapter<StorageProfileS3Dto> {

    private final HubSession session;

    public HubStorageProfileListService(final HubSession session) {
        this.session = session;
    }

    /**
     * @return IDs of storage profiles
     */
    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        try {
            final List<StorageProfileS3Dto> storageProfiles = new StorageProfileResourceApi(session.getClient()).apiStorageprofileS3Get();
            return new AttributedList<>(
                    storageProfiles.stream().map(model ->
                                    new Path(model.getName(), EnumSet.of(AbstractPath.Type.file))
                                            .withAttributes(this.toAttributes(model))
                            )
                            .collect(Collectors.toList()));
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    public PathAttributes toAttributes(final StorageProfileS3Dto model) {
        return new PathAttributes().withFileId(model.getId().toString());
    }

    @Override
    public void preflight(final Path directory) throws BackgroundException {
        //
    }

    @Override
    public InputStream read(final Path file, final TransferStatus status, final ConnectionCallback callback) throws BackgroundException {
        try {
            final StorageProfileDto storageProfile = new StorageProfileResourceApi(session.getClient())
                    .apiStorageprofileProfileIdGet(UUID.fromString(file.attributes().getFileId()));
            final ConfigDto configDto = new ConfigResourceApi(session.getClient()).apiConfigGet();
            final Protocol profileProtocol = toProfileParentProtocol(storageProfile, configDto);
            final String content = profileProtocol.serialize(new PlistSerializer()).toXMLPropertyList();
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
        catch(StorageProfileDtoWrapperException e) {
            throw new InteroperabilityException(e.getMessage(), e);
        }
    }

    public class StorageProfileFindFeature implements Find {
        @Override
        public boolean find(final Path file, final ListProgressListener listener) throws BackgroundException {
            try {
                new StorageProfileAttributesFinder().find(file, listener);
                return true;
            }
            catch(NotfoundException e) {
                return false;
            }
        }
    }

    public class StorageProfileAttributesFinder implements AttributesFinder, AttributesAdapter<StorageProfileDto> {
        @Override
        public PathAttributes find(final Path file, final ListProgressListener listener) throws BackgroundException {
            try {
                return this.toAttributes(new StorageProfileResourceApi(session.getClient())
                        .apiStorageprofileProfileIdGet(UUID.fromString(file.attributes().getFileId())));
            }
            catch(ApiException e) {
                throw new HubExceptionMappingService().map(e);
            }
        }

        @Override
        public PathAttributes toAttributes(final StorageProfileDto model) {
            return new PathAttributes().withFileId(model.getStorageProfileS3Dto().getId().toString());
        }
    }
}
