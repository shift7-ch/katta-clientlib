/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.model;

public class AccountKeyAndDeviceName {
    private String accountKey;
    private String deviceName;
    private boolean addToKeychain;

    public String accountKey() {
        return accountKey;
    }

    public String deviceName() {
        return deviceName;
    }

    public boolean addToKeychain() {
        return addToKeychain;
    }

    public AccountKeyAndDeviceName withAccountKey(final String accountKey) {
        this.accountKey = accountKey;
        return this;
    }

    public AccountKeyAndDeviceName withDeviceName(final String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public AccountKeyAndDeviceName withAddToKeychain(final boolean addToKeychain) {
        this.addToKeychain = addToKeychain;
        return this;
    }
}
