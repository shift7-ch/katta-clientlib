/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage.minio;


import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import cloud.katta.cli.Katta;
import cloud.katta.testsetup.AbtractAdminCLIIT;
import cloud.katta.testsetup.CLIIntegrationTest;
import io.minio.admin.MinioAdminClient;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CLIIntegrationTest
class MinioSTSStorageIT extends AbtractAdminCLIIT {

    @Test
    public void testMinioSetup() throws Exception {
        int rc = new CommandLine(new Katta()).execute(
                "minioStsSetup",
                "--endpointUrl", "http://localhost:9100",
                "--hubUrl", "http://localhost:8280",
                "--accessKey", "minioadmin",
                "--secretKey", "minioadmin",
                "--bucketPrefix", "macaraoni",
                "--roleNamePrefix", "fusilli"
        );
        assertEquals(0, rc);

        final MinioAdminClient minioAdminClient = new MinioAdminClient.Builder()
                .credentials("minioadmin", "minioadmin")
                .endpoint("http://localhost:9100").build();

        final Map<String, String> cannedPolicies = minioAdminClient.listCannedPolicies();
        {
            final JSONObject miniocreatebucketpolicy = new JSONObject(cannedPolicies.get("fusillicreatebucketpolicy"));
            final JSONArray statements = miniocreatebucketpolicy.getJSONArray("Statement");
            long count = 0;
            for(int i = 0; i < statements.length(); i++) {
                count += statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).filter(s -> s.contains("macaraoni")).count();
            }
            assertEquals(3, count);
        }
        {
            final JSONObject minioaccessbucket = new JSONObject(cannedPolicies.get("fusilliaccessbucketpolicy"));
            final JSONArray statements = minioaccessbucket.getJSONArray("Statement");
            long count = 0;
            for(int i = 0; i < statements.length(); i++) {
                count += statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).filter(s -> s.contains("macaraoni")).count();
            }
            assertEquals(2, count);
        }
    }
}
