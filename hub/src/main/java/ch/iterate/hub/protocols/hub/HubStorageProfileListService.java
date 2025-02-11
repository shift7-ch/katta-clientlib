
/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.ConnectionCallback;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.io.MD5ChecksumCompute;
import ch.cyberduck.core.serializer.impl.dd.PlistSerializer;
import ch.cyberduck.core.transfer.TransferStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.protocols.hub.exceptions.HubExceptionMappingService;
import ch.iterate.hub.protocols.hub.serializer.HubConfigDtoDeserializer;
import ch.iterate.hub.protocols.hub.serializer.StorageProfileDtoWrapperDeserializer;

public class HubStorageProfileListService implements ListService, Read {

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
            final ConfigDto configDto = new ConfigResourceApi(session.getClient()).apiConfigGet();
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(session.getClient()).apiStorageprofileGet(false);
            final AttributedList<Path> list = new AttributedList<>();
            for(final StorageProfileDto storageProfile : storageProfiles) {
                final StorageProfileDtoWrapper wrapper = StorageProfileDtoWrapper.coerce(storageProfile);
                list.add(new Path(String.format("%s (%s)", wrapper.getName(), wrapper.getId()), EnumSet.of(AbstractPath.Type.file))
                        .withAttributes(new StorageProfileAttributesFinder().toAttributes(configDto, wrapper)));
            }
            return list;
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    public void preflight(final Path directory) throws BackgroundException {
        //
    }

    @Override
    public InputStream read(final Path file, final TransferStatus status, final ConnectionCallback callback) throws BackgroundException {
        try {
            final ConfigDto configDto = new ConfigResourceApi(session.getClient()).apiConfigGet();
            final StorageProfileDto storageProfile = new StorageProfileResourceApi(session.getClient())
                    .apiStorageprofileProfileIdGet(UUID.fromString(file.attributes().getFileId()));
            return new ByteArrayInputStream(serialize(configDto, StorageProfileDtoWrapper.coerce(storageProfile)).getBytes(StandardCharsets.UTF_8));
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
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

    public class StorageProfileAttributesFinder implements AttributesFinder {
        @Override
        public PathAttributes find(final Path file, final ListProgressListener listener) throws BackgroundException {
            try {
                final StorageProfileDto storageProfile = new StorageProfileResourceApi(session.getClient())
                        .apiStorageprofileProfileIdGet(UUID.fromString(file.attributes().getFileId()));
                return this.toAttributes(StorageProfileDtoWrapper.coerce(storageProfile));
            }
            catch(ApiException e) {
                throw new HubExceptionMappingService().map(e);
            }
        }

        public PathAttributes toAttributes(final StorageProfileDtoWrapper wrapper) throws BackgroundException {
            try {
                final ConfigDto configDto = new ConfigResourceApi(session.getClient()).apiConfigGet();
                return this.toAttributes(configDto, wrapper);
            }
            catch(ApiException e) {
                throw new HubExceptionMappingService().map(e);
            }
        }

        public PathAttributes toAttributes(final ConfigDto configDto, final StorageProfileDtoWrapper wrapper) throws BackgroundException {
            return new PathAttributes().withFileId(wrapper.getId().toString())
                    .withChecksum(new MD5ChecksumCompute().compute(new ByteArrayInputStream(serialize(configDto, wrapper).getBytes(StandardCharsets.UTF_8)), new TransferStatus()));
        }
    }

    private static String serialize(final ConfigDto configDto, final StorageProfileDtoWrapper storageProfile) throws InteroperabilityException {
        switch(storageProfile.getProtocol()) {
            case S3:
            case S3_STS:
                return new Profile(ProtocolFactory.get().forScheme(Scheme.s3), new StorageProfileDtoWrapperDeserializer(
                        new HubConfigDtoDeserializer(configDto), storageProfile)).serialize(new PlistSerializer()).toXMLPropertyList();
            default:
                throw new InteroperabilityException(String.format("Unsupported storage configuration %s", storageProfile.getProtocol().name()));
        }
    }
}
