/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.model;


public class VaultSpec {
    public final String storageProfileName;
    public final String storageProfileId;
    public final String bucketName;
    public final String username;
    public final String password;

    public VaultSpec(final String storageProfileName, final String storageProfileId, final String bucketName, final String username, final String password) {
        this.storageProfileName = storageProfileName;
        this.storageProfileId = storageProfileId;
        this.bucketName = bucketName;
        this.username = username;
        this.password = password;
    }
}


