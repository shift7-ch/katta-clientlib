/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.model;

public class HubTestSetupUserConfig {

    public final String username;
    public final String password;
    public final String setupCode;


    public HubTestSetupUserConfig(final String username, final String password, final String setupCode) {
        this.username = username;
        this.password = password;
        this.setupCode = setupCode;
    }

    @Override
    public String toString() {
        return "HubTestSetupUserConfig{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", setupCode='" + setupCode + '\'' +
                '}';
    }
}
