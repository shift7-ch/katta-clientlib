/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core.callback;

import java.util.UUID;

public class CreateVaultModel {

    private final UUID uuid;
    private String reason;
    private final String vaultName;
    private final String vaultDescription;
    private final String backend;
    private final String accessKeyId;
    private final String secretKey;
    private final String bucketName;
    private final String region;
    private final boolean automaticAccessGrant;
    private final int maxWotLevel;


    public CreateVaultModel(final UUID uuid, final String reason, final String vaultName, final String vaultDescription, final String backend, final String accessKeyId, final String secretKey, final String bucketName, final String region, final boolean automaticAccessGrant, final int maxWotLevel) {
        this.uuid = uuid;
        this.reason = reason;
        this.vaultName = vaultName;
        this.vaultDescription = vaultDescription;
        this.backend = backend;
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = region;
        this.automaticAccessGrant = automaticAccessGrant;
        this.maxWotLevel = maxWotLevel;
    }

    public UUID uuid() {
        return uuid;
    }

    public String reason() {
        return reason;
    }

    public CreateVaultModel withReason(final String reason) {
        this.reason = reason;
        return this;
    }

    public String vaultName() {
        return vaultName;
    }

    public String vaultDescription() {
        return vaultDescription;
    }

    public String backend() {
        return backend;
    }

    public String storageProfileId() {
        return backend;
    }

    public String accessKeyId() {
        return accessKeyId;
    }

    public String secretKey() {
        return secretKey;
    }

    public String bucketName() {
        return bucketName;
    }

    public String region() {
        return region;
    }

    public boolean automaticAccessGrant() {
        return automaticAccessGrant;
    }

    public int maxWotLevel() {
        return maxWotLevel;
    }
}
