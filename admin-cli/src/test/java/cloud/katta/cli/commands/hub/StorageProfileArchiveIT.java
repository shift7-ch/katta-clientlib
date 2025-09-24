/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;


import ch.cyberduck.core.PasswordStoreFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;

import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.HubTestConfig;

class StorageProfileArchiveIT extends AbtractAdminCliIT {
    private static final Logger log = LogManager.getLogger(StorageProfileArchiveIT.class.getName());
    public static ComposeContainer compose;


    @Test
    public void testHubWorkflow() throws Exception {
        HubTestConfig.Setup config = new HubTestConfig.Setup().withUserConfig(
                new HubTestConfig.Setup.UserConfig("admin", "admin", "setupcode")
        ).withHubURL("http://localhost:8280");
        final HubSession hubSession = setupConnection(config);
        final String accessToken = PasswordStoreFactory.get().findOAuthTokens(hubSession.getHost()).getAccessToken();

        new StorageProfileArchive().call("http://localhost:8280", accessToken, "732D43FA-3716-46C4-B931-66EA5405EF1C");
    }
}
