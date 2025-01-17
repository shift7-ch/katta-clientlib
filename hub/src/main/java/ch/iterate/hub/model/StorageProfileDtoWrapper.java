/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.model;

import java.util.List;
import java.util.UUID;

import ch.iterate.hub.client.model.S3STORAGECLASSES;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;

/**
 * openapi-generator does not generate sub-classes
 */
public class StorageProfileDtoWrapper {
    private final StorageProfileDto proxy; // ProfileS3Dto or ProfileS3StsDto

    private StorageProfileDtoWrapper(final StorageProfileDto proxy) {
        this.proxy = proxy;
    }

    public static StorageProfileDtoWrapper coerce(StorageProfileDto o) {
        return new StorageProfileDtoWrapper(o);
    }

    public Class getType() {
        return proxy.getActualInstance().getClass();
    }

    public ch.iterate.hub.client.model.Protocol getProtocol() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getProtocol();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getProtocol();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public UUID getId() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getId();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getId();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getName() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getName();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getName();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getHostname() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getHostname();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getHostname();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getScheme() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getScheme();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getScheme();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public Integer getPort() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getPort();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getPort();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getStsEndpoint() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsEndpoint();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public Boolean getBucketAcceleration() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getBucketAcceleration();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getBucketPrefix() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getBucketPrefix();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getRegion() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getRegion();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public List<String> getRegions() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getRegions();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getStsRoleArn() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsRoleArn();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getStsRoleArn2() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsRoleArn2();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public String getStsRoleArnClient() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsRoleArnClient();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public Integer getStsDurationSeconds() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsDurationSeconds();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public Boolean getWithPathStyleAccessEnabled() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getWithPathStyleAccessEnabled();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getWithPathStyleAccessEnabled();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }

    public S3STORAGECLASSES getStorageClass() throws StorageProfileDtoWrapperException {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getStorageClass();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStorageClass();
        }
        throw new StorageProfileDtoWrapperException(String.format("Expected %s be either StorageProfileS3Dto or StorageProfileS3STSDto. Found %s", proxy, proxy.getClass()));
    }
}