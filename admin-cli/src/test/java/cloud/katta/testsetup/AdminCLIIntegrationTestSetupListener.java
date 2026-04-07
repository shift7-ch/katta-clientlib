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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

public class AdminCLIIntegrationTestSetupListener implements TestExecutionListener {
    private static final Logger log = LogManager.getLogger(AdminCLIIntegrationTestSetupListener.class);
    private static ComposeContainer<?> compose;


    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if(testPlan.getRoots().stream()
                .flatMap(root -> testPlan.getChildren(root).stream())
                .filter(ti -> ti.getTags().contains(TestTag.create("cli")))
                .findAny().isPresent()) {

            final String composeFile = "/docker-compose-minio-localhost-hub.yml";
            final String envFile = "/.local.env";
            final String profile = "local";
            final String hubAdminUser = "admin";
            final String hubAdminPassword = "admin";
            final String hubKeycloakSystemClientSecret = "top-secret";
            final Properties props = new Properties();
            try {
                props.load(AbstractAdminCLIIT.class.getResourceAsStream(envFile));
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
            final HashMap<String, String> env = props.entrySet().stream().collect(
                    Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> String.valueOf(e.getValue()),
                            (prev, next) -> next, HashMap::new
                    ));
            env.put("HUB_ADMIN_USER", hubAdminUser);
            env.put("HUB_ADMIN_PASSWORD", hubAdminPassword);
            env.put("HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET", hubKeycloakSystemClientSecret);
            try {
                compose = new ComposeContainer(
                        new File(AbstractAdminCLIIT.class.getResource(composeFile).toURI()))
                        .withPull(true)
                        .withEnv(env)
                        .withOptions(String.format("--profile=%s", profile))
                        .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*Completed MinIO Setup.*").withStartupTimeout(Duration.ofMinutes(2)));
            }
            catch(URISyntaxException e) {
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
