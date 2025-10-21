/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.model;

import java.util.List;
import java.util.UUID;

import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3Dto;
import cloud.katta.client.model.StorageProfileS3STSDto;

/**
 * openapi-generator does not generate sub-classes
 */
public class StorageProfileDtoWrapper {
    private final StorageProfileDto proxy; // ProfileS3Dto or ProfileS3StsDto

    private StorageProfileDtoWrapper(final StorageProfileDto proxy) {
        this.proxy = proxy;
    }

    public static StorageProfileDtoWrapper coerce(final StorageProfileDto o) {
        return new StorageProfileDtoWrapper(o);
    }

    public Class getType() {
        return proxy.getActualInstance().getClass();
    }

    public Protocol getProtocol() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getProtocol();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getProtocol();
        }
        return null;
    }

    public UUID getId() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getId();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getId();
        }
        return null;
    }

    public String getName() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getName();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getName();
        }
        return null;
    }

    public String getHostname() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getHostname();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getHostname();
        }
        return null;
    }

    public String getScheme() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getScheme();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getScheme();
        }
        return null;
    }

    public Integer getPort() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getPort();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getPort();
        }
        return null;
    }

    public String getStsEndpoint() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsEndpoint();
        }
        return null;
    }

    public Boolean getBucketAcceleration() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getBucketAcceleration();
        }
        return null;
    }

    public String getBucketPrefix() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getBucketPrefix();
        }
        return null;
    }

    public String getRegion() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getRegion();
        }
        return null;
    }

    public List<String> getRegions() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getRegions();
        }
        return null;
    }

    public String getStsRoleAccessBucketAssumeRoleWithWebIdentity() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsRoleAccessBucketAssumeRoleWithWebIdentity();
        }
        return null;
    }

    public String getStsRoleAccessBucketAssumeRoleTaggedSession() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsRoleAccessBucketAssumeRoleTaggedSession();
        }
        return null;
    }

    public String getStsRoleCreateBucketClient() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsRoleCreateBucketClient();
        }
        return null;
    }

    public Integer getStsDurationSeconds() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsDurationSeconds();
        }
        return null;
    }

    public String getStsSessionTag() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            // only STS
            return null;
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStsSessionTag();
        }
        return null;
    }

    public Boolean getWithPathStyleAccessEnabled() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getWithPathStyleAccessEnabled();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getWithPathStyleAccessEnabled();
        }
        return null;
    }

    public S3STORAGECLASSES getStorageClass() {
        if(proxy.getActualInstance() instanceof StorageProfileS3Dto) {
            return ((StorageProfileS3Dto) proxy.getActualInstance()).getStorageClass();
        }
        else if(proxy.getActualInstance() instanceof StorageProfileS3STSDto) {
            return ((StorageProfileS3STSDto) proxy.getActualInstance()).getStorageClass();
        }
        return null;
    }
}
