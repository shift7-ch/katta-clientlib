/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.model;

public class AccountKeyAndDeviceName {
    private String accountKey;

    private String deviceName;

    public AccountKeyAndDeviceName() {
    }

    public String accountKey() {
        return accountKey;
    }

    public String deviceName() {
        return deviceName;
    }

    public AccountKeyAndDeviceName withAccountKey(final String accountKey) {
        this.accountKey = accountKey;
        return this;
    }

    public AccountKeyAndDeviceName withDeviceName(final String deviceName) {
        this.deviceName = deviceName;
        return this;
    }
}
