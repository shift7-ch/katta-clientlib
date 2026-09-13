/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Docker Compose environment from <a href="https://github.com/shift7-ch/katta-compose">katta-compose</a> configured
 * with the test environment of this project.
 */
public final class KattaCompose {

    private KattaCompose() {
    }

    /**
     * @return Compose file of this project including katta-compose
     */
    public static File composeFile() {
        return resource("/compose.yaml");
    }

    /**
     * @param envFile Classpath resource with variables for the compose file
     * @return Variables from the env file with the Keycloak realm and setup files of this project
     */
    public static Map<String, String> environment(final String envFile) throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = Objects.requireNonNull(KattaCompose.class.getResourceAsStream(envFile), envFile)) {
            properties.load(in);
        }
        final Map<String, String> env = new HashMap<>();
        for(String name : properties.stringPropertyNames()) {
            env.put(name, properties.getProperty(name));
        }
        env.put("KEYCLOAK_REALM_FILE", resource("/keycloak/cryptomator-realm.json").getAbsolutePath());
        env.put("SETUP_DIR", resource("/setup").getAbsolutePath());
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
