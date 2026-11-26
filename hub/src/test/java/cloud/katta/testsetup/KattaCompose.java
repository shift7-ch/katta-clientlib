/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Docker Compose environment from the deployment examples in
 * <a href="https://github.com/shift7-ch/katta-server/tree/develop/deploy/compose">katta-server</a> configured with
 * the test environment of this project.
 */
public final class KattaCompose {

    private KattaCompose() {
    }

    /**
     * @param envFile  Classpath resource with variables for the compose file
     * @param scenario Scenario to start, selects the compose file {@code /compose-<scenario>.yaml}
     * @return Compose container that waits for Katta Server to be healthy when started
     */
    public static ComposeContainer container(final String envFile, final String scenario) throws IOException {
        return new ComposeContainer(composeFile(scenario))
                .withPull(true)
                .withEnv(environment(envFile))
                .waitingFor("hub", new DockerHealthcheckWaitStrategy());
    }

    /**
     * @param scenario Scenario to start
     * @return Compose file of this project including the scenario of katta-server
     */
    private static File composeFile(final String scenario) {
        return resource(String.format("/compose-%s.yaml", scenario));
    }

    /**
     * @param envFile Classpath resource with variables for the compose file
     * @return Variables of the env file
     */
    public static Properties properties(final String envFile) throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = Objects.requireNonNull(KattaCompose.class.getResourceAsStream(envFile), envFile)) {
            properties.load(in);
        }
        return properties;
    }

    /**
     * @param envFile Classpath resource with variables for the compose file
     * @return Variables from the env file
     */
    private static Map<String, String> environment(final String envFile) throws IOException {
        final Properties properties = properties(envFile);
        final Map<String, String> env = new HashMap<>();
        for(String name : properties.stringPropertyNames()) {
            env.put(name, properties.getProperty(name));
        }
        return env;
    }

    private static File resource(final String name) {
        try {
            return new File(Objects.requireNonNull(KattaCompose.class.getResource(name), name).toURI());
        }
        catch(URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
