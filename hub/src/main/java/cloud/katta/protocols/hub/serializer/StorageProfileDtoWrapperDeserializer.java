/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub.serializer;

import ch.cyberduck.core.serializer.Deserializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cloud.katta.client.model.Protocol;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;
import com.dd.plist.NSDictionary;

import static ch.cyberduck.core.Profile.*;

public class StorageProfileDtoWrapperDeserializer extends ProxyDeserializer<NSDictionary> {
    private static final Logger log = LogManager.getLogger(StorageProfileDtoWrapperDeserializer.class);

    private final StorageProfileDtoWrapper dto;

    public StorageProfileDtoWrapperDeserializer(final StorageProfileDtoWrapper dto) {
        this(dto, ProxyDeserializer.empty());
    }

    /**
     * @param dto Storage configuration
     */
    public StorageProfileDtoWrapperDeserializer(final StorageProfileDtoWrapper dto, final Deserializer<NSDictionary> parent) {
        super(parent);
        this.dto = dto;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <L> List<L> listForKey(final String key) {
        switch(key) {
            case PROPERTIES_KEY:
                // In format key=value
                final List<String> properties = new ArrayList<>(super.listForKey(key));
                if(dto.getWithPathStyleAccessEnabled()) {
                    properties.add(String.format("s3.bucket.virtualhost.disable=%s", true));
                }
                if(dto.getStorageClass() != null) {
                    properties.add(String.format("3.storage.class.options=%s", dto.getStorageClass().name()));
                    properties.add(String.format("3.storage.class=%s", dto.getStorageClass().name()));
                }
                if(dto.getProtocol() == Protocol.S3_STS) {
                    properties.add(String.format("%s=%s", S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE, true));
                    properties.add(String.format("%s=%s", S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN, dto.getStsRoleArn()));
                    if(dto.getStsRoleArn2() != null) {
                        properties.add(String.format("%s=%s", S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_2, dto.getStsRoleArn2()));
                    }
                    if(dto.getStsDurationSeconds() != null) {
                        properties.add(String.format("%s=%s", S3AssumeRoleProtocol.S3_ASSUMEROLE_DURATIONSECONDS, dto.getStsDurationSeconds().toString()));
                    }
                }
                log.debug("Return properties {} from {}", properties, dto);
                return (List<L>) properties;
            case REGIONS_KEY:
                return (List<L>) dto.getRegions();
        }
        return super.listForKey(key);
    }

    @Override
    public String stringForKey(final String key) {
        // default profile and possible regions for UI:
        switch(key) {
            case PROTOCOL_KEY:
                switch(dto.getProtocol()) {
                    case S3:
                    case S3_STS:
                        return new S3AssumeRoleProtocol().getIdentifier();
                }
                break;
            case DEFAULT_NICKNAME_KEY:
                return dto.getName();
            case SCHEME_KEY:
                return dto.getScheme();
            case DEFAULT_HOSTNAME_KEY:
                return dto.getHostname();
            case DEFAULT_PORT_KEY:
                return String.valueOf(dto.getPort());
            case STS_ENDPOINT_KEY:
                return dto.getStsEndpoint();
            case REGION_KEY:
                return dto.getRegion();
        }
        return super.stringForKey(key);
    }

    @Override
    public Boolean booleanForKey(final String key) {
        switch(key) {
            case OAUTH_CONFIGURABLE_KEY:
                switch(dto.getProtocol()) {
                    case S3:
                        return false;
                    case S3_STS:
                        return true;
                }
                break;
        }
        return super.booleanForKey(key);
    }

    @Override
    public List<String> keys() {
        final List<String> keys = new ArrayList<>(super.keys());
        keys.addAll(Arrays.asList(
                PROTOCOL_KEY,
                PROPERTIES_KEY,
                OAUTH_CONFIGURABLE_KEY)
        );
        if(dto.getName() != null) {
            keys.add(DEFAULT_NICKNAME_KEY);
        }
        if(dto.getScheme() != null) {
            keys.add(SCHEME_KEY);
        }
        if(dto.getHostname() != null) {
            keys.add(DEFAULT_HOSTNAME_KEY);
        }
        if(dto.getPort() != null) {
            keys.add(DEFAULT_PORT_KEY);
        }
        if(dto.getStsEndpoint() != null) {
            keys.add(STS_ENDPOINT_KEY);
        }
        if(dto.getRegion() != null) {
            keys.add(REGION_KEY);
        }
        if(dto.getRegions() != null) {
            keys.add(REGIONS_KEY);
        }
        return keys;
    }
}
