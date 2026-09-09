/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.client;

import ch.cyberduck.core.Host;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import cloud.katta.protocols.hub.HubProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HubApiClientTest {

    @Test
    void testBasePathWithoutDefaultPath() throws Exception {
        final Host host = new Host(new HubProtocol(), "hub.example.com");
        try(final CloseableHttpClient client = HttpClients.createMinimal()) {
            assertEquals("https://hub.example.com", new HubApiClient(host, client).getBasePath());
        }
    }

    @Test
    void testBasePathWithRootDefaultPath() throws Exception {
        final Host host = new Host(new HubProtocol(), "hub.example.com");
        host.setDefaultPath("/");
        try(final CloseableHttpClient client = HttpClients.createMinimal()) {
            // No trailing slash, otherwise operation paths would be appended as "//api/config"
            assertEquals("https://hub.example.com", new HubApiClient(host, client).getBasePath());
        }
    }

    @Test
    void testBasePathWithDefaultPath() throws Exception {
        final Host host = new Host(new HubProtocol(), "hub.example.com");
        host.setDefaultPath("/hub");
        try(final CloseableHttpClient client = HttpClients.createMinimal()) {
            assertEquals("https://hub.example.com/hub", new HubApiClient(host, client).getBasePath());
        }
    }
}
