/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.IOException;
import java.time.Duration;

public class AdminCLIIntegrationTestSetupListener implements TestExecutionListener {
    private static final Logger log = LogManager.getLogger(AdminCLIIntegrationTestSetupListener.class);
    private static ComposeContainer compose;


    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if(testPlan.getRoots().stream()
                .flatMap(root -> testPlan.getChildren(root).stream())
                .anyMatch(ti -> ti.getTags().contains(TestTag.create("cli")))) {

            try {
                compose = KattaCompose.container("/.local.env", "local");
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
            compose.start();
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            if(compose != null) {
                compose.stop();
            }
        }
        catch(Exception e) {
            log.warn("Failed to stop docker-compose test environment", e);
        }
    }
}
