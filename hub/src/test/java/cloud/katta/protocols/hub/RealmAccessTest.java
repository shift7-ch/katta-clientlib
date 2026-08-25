/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Host;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cloud.katta.client.model.RealmRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class RealmAccessTest {

    @Test
    void testCreateVaultsRoleAssigned() {
        final Host host = new Host(new HubProtocol());
        assertTrue(HubSession.RealmAccess.parse(host, token(Collections.singletonMap("roles", Arrays.asList("user", "create-vaults"))))
                .contains(RealmRole.CREATE_VAULTS.getValue()));

    }

    @Test
    void testCreateVaultsRoleNotAssigned() {
        final Host host = new Host(new HubProtocol());
        assertFalse(HubSession.RealmAccess.parse(host, token(Collections.singletonMap("roles", Collections.singletonList("user"))))
                .contains(RealmRole.CREATE_VAULTS.getValue()));
    }

    @Test
    void testNoRolesAssigned() {
        final Host host = new Host(new HubProtocol());
        assertFalse(HubSession.RealmAccess.parse(host, token(Collections.singletonMap("roles", Collections.emptyList())))
                .contains(RealmRole.CREATE_VAULTS.getValue()));
    }

    @Test
    void testUnknownMembersInClaimIgnored() {
        final Host host = new Host(new HubProtocol());
        final Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Collections.singletonList("create-vaults"));
        realmAccess.put("unknown", "ignored");
        assertTrue(HubSession.RealmAccess.parse(host, token(realmAccess))
                .contains(RealmRole.CREATE_VAULTS.getValue()));
    }

    @Test
    void testClaimWithoutRolesMember() {
        final Host host = new Host(new HubProtocol());
        assertTrue(HubSession.RealmAccess.parse(host, token(Collections.singletonMap("unknown", "ignored"))).isEmpty());
    }

    @Test
    void testClaimMissing() {
        final Host host = new Host(new HubProtocol());
        assertTrue(HubSession.RealmAccess.parse(host, JWT.create().withSubject("alice").sign(Algorithm.none())).isEmpty());
    }

    @Test
    void testClaimNull() {
        final Host host = new Host(new HubProtocol());
        assertTrue(HubSession.RealmAccess.parse(host, JWT.create().withNullClaim("realm_access").sign(Algorithm.none())).isEmpty());
    }

    @Test
    void testNoFailureForInvalidToken() {
        final Host host = new Host(new HubProtocol());
        assertTrue(HubSession.RealmAccess.parse(host, "not-a-jwt").isEmpty());
    }

    /**
     * Access token as issued by Keycloak with <code>realm_access</code> claim mapped from realm roles
     */
    private static String token(final Map<String, ?> realmAccess) {
        return JWT.create()
                .withSubject("alice")
                .withClaim("realm_access", realmAccess)
                .sign(Algorithm.none());
    }
}
