/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.setup;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import cloud.katta.cli.KattaSetupCli;
import io.minio.admin.MinioAdminClient;
import picocli.CommandLine;

/**
 * Sets up MinIO for Katta in STS mode:
 * <ul>
 *  <li>creates/updates OIDC provider for cryptomator, cryptomatorhub and cryptomatorvaults clients.</li>
 *  <li>creates roles and role policy for
 *      <ul>
 *          <li>creating vaults: access restricted to creating buckets with given prefix</li>
 *          <li>accessing vaults: access restricted to reading/writing to single bucket.</li>
 *      </ul>
 *  </li>
 * </ul>
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#setup-aws">Katta Docs</a>.
 */
@CommandLine.Command(name = "minioSetup", description = "Setup/update OIDC provider and roles for STS in MinIO.", mixinStandardHelpOptions = true)
public class MinioStsSetup implements Callable<Void> {
    @CommandLine.Option(names = {"--endpointUrl"}, description = "MinIO URL. Example: \"http://localhost:9000\"", required = true)
    String endpointUrl;

    @CommandLine.Option(names = {"--profileName"}, description = "AWS profile to load AWS credentials from. See ~/.aws/credentials.", required = false)
    String profileName;

    @CommandLine.Option(names = {"--accessKey"}, description = "Access Key for administering MinIO if no profile is used.", required = false)
    String accessKey;

    @CommandLine.Option(names = {"--secretKey"}, description = "Secret Key for administering MinIO if no profile is used.", required = false)
    String secretKey;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket Prefix for STS vaults.", required = false, defaultValue = "katta")
    String bucketPrefix;

    @CommandLine.Option(names = {"--maxSessionDuration"}, description = "Bucket Prefix for STS vaults.", required = false)
    Integer maxSessionDuration;

    @Override
    public Void call() throws Exception {
        final String createbucketPolicyName = "cipherduckcreatebucket";

        final JSONObject miniocreatebucketpolicy = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/minio_sts/createbucketpolicy.json"), Charset.defaultCharset()));
        final MinioAdminClient minioAdminClient = new MinioAdminClient.Builder()
                .credentials(accessKey, secretKey)
                .endpoint(endpointUrl).build();

        //        /mc admin policy create myminio cipherduckcreatebucket /setup/minio_sts/createbucketpolicy.json
        final JSONArray statements = miniocreatebucketpolicy.getJSONArray("Statement");
        for(int i = 0; i < statements.length(); i++) {
            final List<String> list = statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).map(s -> s.replace("katta", bucketPrefix)).toList();
            statements.getJSONObject(i).put("Resource", list);
        }
        minioAdminClient.addCannedPolicy(createbucketPolicyName, miniocreatebucketpolicy.toString());
        System.out.println(minioAdminClient.listCannedPolicies().get(createbucketPolicyName));


//                /mc admin policy create myminio cipherduckaccessbucket /setup/minio_sts/accessbucketpolicy.json
//
//
//                /mc idp openid add myminio cryptomator \
//        config_url="${HUB_KEYCLOAK_URL}${HUB_KEYCLOAK_BASEPATH}/realms/${HUB_KEYCLOAK_REALM}/.well-known/openid-configuration" \
//        client_id="cryptomator" \
//        client_secret="ignore-me" \
//        role_policy="cipherduckcreatebucket"
//                /mc idp openid add myminio cryptomatorhub \
//        config_url="${HUB_KEYCLOAK_URL}${HUB_KEYCLOAK_BASEPATH}/realms/${HUB_KEYCLOAK_REALM}/.well-known/openid-configuration" \
//        client_id="cryptomatorhub" \
//        client_secret="ignore-me" \
//        role_policy="cipherduckcreatebucket"
//                /mc idp openid add myminio cryptomatorvaults \
//        config_url="${HUB_KEYCLOAK_URL}${HUB_KEYCLOAK_BASEPATH}/realms/${HUB_KEYCLOAK_REALM}/.well-known/openid-configuration" \
//        client_id="cryptomatorvaults" \
//        client_secret="ignore-me" \
//        role_policy="cipherduckaccessbucket"

        return null;
    }
}
