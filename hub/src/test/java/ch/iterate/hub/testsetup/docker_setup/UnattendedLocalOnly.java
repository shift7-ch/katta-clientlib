/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.docker_setup;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.URISyntaxException;

import ch.iterate.hub.testsetup.HubTestUtilities;

import static ch.iterate.hub.testsetup.HubTestSetupConfigs.UNATTENDED_LOCAL_ONLY;

public class UnattendedLocalOnly extends HubTestSetupDockerExtension {
    @Override
    public void beforeAll(ExtensionContext context) throws IOException, URISyntaxException {
        HubTestUtilities.preferences();
        this.setupDocker(UNATTENDED_LOCAL_ONLY.dockerConfig());
    }
}
