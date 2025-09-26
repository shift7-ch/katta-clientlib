/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage;

import cloud.katta.cli.KattaSetupCli;
import cloud.katta.testcontainers.AbtractAdminCliIT;
import io.minio.admin.MinioAdminClient;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.Map;
import java.util.Objects;

class MinioStsSetupIT extends AbtractAdminCliIT {

    @Test
    public void testMinioSetup() throws Exception {
        int rc = new CommandLine(new KattaSetupCli()).execute(
                "minioStsSetup",
                "--endpointUrl", "http://localhost:9100",
                "--hubUrl", "http://localhost:8280",
                "--accessKey", "minioadmin",
                "--secretKey", "minioadmin",
                "--bucketPrefix", "fusilli"
        );
        assertEquals(0, rc);

        final MinioAdminClient minioAdminClient = new MinioAdminClient.Builder()
                .credentials("minioadmin", "minioadmin")
                .endpoint("http://localhost:9100").build();

        final Map<String, String> cannedPolicies = minioAdminClient.listCannedPolicies();
        {
            final JSONObject miniocreatebucketpolicy = new JSONObject(cannedPolicies.get("fusillicreatebucketpolicy"));
            final JSONArray statements = miniocreatebucketpolicy.getJSONArray("Statement");
            int count = 0;
            for (int i = 0; i < statements.length(); i++) {
                count += statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).map(s -> s.contains("fusilli")).count();
            }
            assertEquals(3, count);
        }
        {
            final JSONObject minioaccessbucket = new JSONObject(cannedPolicies.get("fusilliaccessbucketpolicy"));
            final JSONArray statements = minioaccessbucket.getJSONArray("Statement");
            int count = 0;
            for (int i = 0; i < statements.length(); i++) {
                count += statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).map(s -> s.contains("fusilli")).count();
            }
            assertEquals(2, count);
        }
    }
}
