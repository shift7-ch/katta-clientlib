/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Home;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestSetupDockerExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({HubTestSetupDockerExtension.LocalAlreadyRunning.class})
class UserKeysServiceImplIT extends AbstractHubTest {

    private static Stream<Arguments> arguments() {
        return Stream.of(LOCAL_MINIO_STATIC);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testSetupCode(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        assertEquals(OAuthTokens.EMPTY, hubSession.getHost().getCredentials().getOauth());
        assertEquals(StringUtils.EMPTY, hubSession.getHost().getCredentials().getPassword());
        final ListService feature = hubSession.getFeature(ListService.class);
        final AttributedList<Path> vaults = feature.list(Home.ROOT, new DisabledListProgressListener());
        assertEquals(2, vaults.size());
        assertEquals(vaults, feature.list(Home.ROOT, new DisabledListProgressListener()));
        for(Path vault : vaults) {
            assertTrue(hubSession.getFeature(Find.class).find(vault));
        }
        new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true);
    }
}
