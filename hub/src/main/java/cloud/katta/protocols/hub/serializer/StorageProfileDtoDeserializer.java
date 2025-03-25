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

import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3Dto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;
import com.dd.plist.NSDictionary;

import static ch.cyberduck.core.Profile.*;

public class StorageProfileDtoDeserializer extends ProxyDeserializer<NSDictionary> {
    private static final Logger log = LogManager.getLogger(StorageProfileDtoDeserializer.class);

    private final StorageProfileDto dto;

    /**
     * @param dto Storage configuration
     */
    public StorageProfileDtoDeserializer(final Deserializer<NSDictionary> parent, final StorageProfileDto dto) {
        super(parent);
        this.dto = dto;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <L> List<L> listForKey(final String key) {
        if(dto.getActualInstance() instanceof StorageProfileS3STSDto) {
            final StorageProfileS3STSDto actualInstance = (StorageProfileS3STSDto) dto.getActualInstance();
            switch(key) {
                case PROPERTIES_KEY:
                    // In format key=value
                    final List<String> properties = new ArrayList<>(super.listForKey(key));
                    if(actualInstance.getWithPathStyleAccessEnabled()) {
                        properties.add(String.format("s3.bucket.virtualhost.disable=%s", true));
                    }
                    if(actualInstance.getStorageClass() != null) {
                        properties.add(String.format("3.storage.class.options=%s", actualInstance.getStorageClass().name()));
                        properties.add(String.format("3.storage.class=%s", actualInstance.getStorageClass().name()));
                    }
                    if(actualInstance.getStsEndpoint() != null) {
                        properties.add(String.format("%s=%s", S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE, true));
                        properties.add(String.format("%s=%s", S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN, actualInstance.getStsRoleArn()));
                        if(actualInstance.getStsRoleArn2() != null) {
                            properties.add(String.format("%s=%s", S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_2, actualInstance.getStsRoleArn2()));
                        }
                        if(actualInstance.getStsDurationSeconds() != null) {
                            properties.add(String.format("%s=%s", S3AssumeRoleProtocol.S3_ASSUMEROLE_DURATIONSECONDS, actualInstance.getStsDurationSeconds().toString()));
                        }
                    }
                    log.debug("Return properties {} from {}", properties, dto);
                    return (List<L>) properties;
                case REGIONS_KEY:
                    return (List<L>) actualInstance.getRegions();
            }
        }
        else if(dto.getActualInstance() instanceof StorageProfileS3Dto) {
            final StorageProfileS3Dto actualInstance = (StorageProfileS3Dto) dto.getActualInstance();
            switch(key) {
                case PROPERTIES_KEY:
                    // In format key=value
                    final List<String> properties = new ArrayList<>(super.listForKey(key));
                    if(actualInstance.getWithPathStyleAccessEnabled()) {
                        properties.add(String.format("s3.bucket.virtualhost.disable=%s", true));
                    }
                    if(actualInstance.getStorageClass() != null) {
                        properties.add(String.format("3.storage.class.options=%s", actualInstance.getStorageClass().name()));
                        properties.add(String.format("3.storage.class=%s", actualInstance.getStorageClass().name()));
                    }
                    log.debug("Return properties {} from {}", properties, dto);
                    return (List<L>) properties;
            }
        }
        return super.listForKey(key);
    }

    @Override
    public String stringForKey(final String key) {
        if(dto.getActualInstance().getClass() == StorageProfileS3Dto.class) {
            final StorageProfileS3Dto actualInstance = (StorageProfileS3Dto) dto.getActualInstance();
            actualInstance.getProtocol();
            // default profile and possible regions for UI:
            switch(key) {
                case PROTOCOL_KEY:
                    switch(actualInstance.getProtocol()) {
                        case S3:
                        case S3_STS:
                            return new S3AssumeRoleProtocol().getIdentifier();
                    }
                    break;
                case VENDOR_KEY:
                    return actualInstance.getId().toString();
                case SCHEME_KEY:
                    return actualInstance.getScheme();
                case DEFAULT_HOSTNAME_KEY:
                    return actualInstance.getHostname();
                case DEFAULT_PORT_KEY:
                    return String.valueOf(actualInstance.getPort());
            }
        }
        else if(dto.getActualInstance().getClass() == StorageProfileS3STSDto.class) {
            final StorageProfileS3STSDto actualInstance = (StorageProfileS3STSDto) dto.getActualInstance();
            // default profile and possible regions for UI:
            switch(key) {
                case PROTOCOL_KEY:
                    switch(actualInstance.getProtocol()) {
                        case S3:
                        case S3_STS:
                            return new S3AssumeRoleProtocol().getIdentifier();
                    }
                    break;
                case VENDOR_KEY:
                    return actualInstance.getId().toString();
                case SCHEME_KEY:
                    return actualInstance.getScheme();
                case DEFAULT_HOSTNAME_KEY:
                    return actualInstance.getHostname();
                case DEFAULT_PORT_KEY:
                    return String.valueOf(actualInstance.getPort());
                case STS_ENDPOINT_KEY:
                    return actualInstance.getStsEndpoint();
                case REGION_KEY:
                    return actualInstance.getRegion();
            }
        }
        return super.stringForKey(key);
    }

    @Override
    public Boolean booleanForKey(final String key) {
        if(dto.getActualInstance().getClass() == StorageProfileS3Dto.class) {
            final StorageProfileS3Dto actualInstance = (StorageProfileS3Dto) dto.getActualInstance();
            switch(key) {
                case OAUTH_CONFIGURABLE_KEY:
                    switch(actualInstance.getProtocol()) {
                        case S3:
                            return false;
                        case S3_STS:
                            return true;
                    }
                    break;
            }
        }
        else if(dto.getActualInstance().getClass() == StorageProfileS3STSDto.class) {
            final StorageProfileS3STSDto actualInstance = (StorageProfileS3STSDto) dto.getActualInstance();
            switch(key) {
                case OAUTH_CONFIGURABLE_KEY:
                    switch(actualInstance.getProtocol()) {
                        case S3:
                            return false;
                        case S3_STS:
                            return true;
                    }
                    break;
            }
        }
        return super.booleanForKey(key);
    }

    @Override
    public List<String> keys() {
        final List<String> keys = new ArrayList<>(super.keys());
        keys.addAll(Arrays.asList(
                PROTOCOL_KEY,
                VENDOR_KEY,
                PROPERTIES_KEY,
                OAUTH_CONFIGURABLE_KEY)
        );
        if(dto.getActualInstance().getClass() == StorageProfileS3Dto.class) {
            final StorageProfileS3Dto actualInstance = (StorageProfileS3Dto) dto.getActualInstance();
            if(actualInstance.getScheme() != null) {
                keys.add(SCHEME_KEY);
            }
            if(actualInstance.getHostname() != null) {
                keys.add(DEFAULT_HOSTNAME_KEY);
            }
            if(actualInstance.getPort() != null) {
                keys.add(DEFAULT_PORT_KEY);
            }
        }
        else if(dto.getActualInstance().getClass() == StorageProfileS3STSDto.class) {
            final StorageProfileS3STSDto actualInstance = (StorageProfileS3STSDto) dto.getActualInstance();
            if(actualInstance.getScheme() != null) {
                keys.add(SCHEME_KEY);
            }
            if(actualInstance.getHostname() != null) {
                keys.add(DEFAULT_HOSTNAME_KEY);
            }
            if(actualInstance.getPort() != null) {
                keys.add(DEFAULT_PORT_KEY);
            }
            if(actualInstance.getStsEndpoint() != null) {
                keys.add(STS_ENDPOINT_KEY);
            }
            if(actualInstance.getRegion() != null) {
                keys.add(REGION_KEY);
            }
            if(actualInstance.getRegions() != null) {
                keys.add(REGIONS_KEY);
            }
        }
        return keys;
    }
}
