/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import cloud.katta.cli.KattaSetupCli;
import cloud.katta.testcontainers.AbtractAdminCliIT;
import io.minio.admin.MinioAdminClient;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinioStsSetupIT extends AbtractAdminCliIT {

    @Test
    public void testStorageProfileAwsStsSetup() throws Exception {
        int rc = new CommandLine(new KattaSetupCli()).execute(
                "minioSetup",
                "--endpointUrl", "http://localhost:9100",
                "--accessKey", "minioadmin",
                "--secretKey", "minioadmin",
                "--bucketPrefix", "farfalle"
        );
        assertEquals(0, rc);

        final MinioAdminClient minioAdminClient = new MinioAdminClient.Builder()
                .credentials("minioadmin", "minioadmin")
                .endpoint("http://localhost:9100").build();
        final JSONObject miniocreatebucketpolicy = new JSONObject(minioAdminClient.listCannedPolicies().get("cipherduckcreatebucket"));
        final JSONArray statements = miniocreatebucketpolicy.getJSONArray("Statement");
        int count = 0;
        for(int i = 0; i < statements.length(); i++) {
            count += statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).map(s -> s.contains("farfalle")).count();
        }
        assertEquals(3, count);
    }
}
