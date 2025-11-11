/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import cloud.katta.cli.KattaSetupCli;
import io.minio.admin.MinioAdminClient;
import picocli.CommandLine;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyWriter;

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
@CommandLine.Command(name = "minioStsSetup", description = "Setup/update OIDC provider and roles for STS in MinIO.", mixinStandardHelpOptions = true)
public class MinioStsSetup implements Callable<Void> {
    @CommandLine.Option(names = {"--endpointUrl"}, description = "MinIO URL. Example: \"http://localhost:9000\"", required = true)
    String endpointUrl;

    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://testing.katta.cloud/tamarind\"", required = true)
    String hubUrl;

    @CommandLine.Option(names = {"--profileName"}, description = "AWS profile to load AWS credentials from. See ~/.aws/credentials.", required = false)
    String profileName;

    @CommandLine.Option(names = {"--accessKey"}, description = "Access Key for administering MinIO if no profile is used.", required = false)
    String accessKey;

    @CommandLine.Option(names = {"--secretKey"}, description = "Secret Key for administering MinIO if no profile is used.", required = false)
    String secretKey;

    @CommandLine.Option(names = {"--bucketPrefix"}, description = "Bucket Prefix for STS vaults.", required = false, defaultValue = "katta-")
    String bucketPrefix;

    @CommandLine.Option(names = {"--maxSessionDuration"}, description = "Bucket Prefix for STS vaults.", required = false)
    Integer maxSessionDuration;

    @CommandLine.Option(names = {"--createbucketPolicyName"}, description = "Policy name for accessing Katta STS buckets. Defaults to {bucketPrefix}createbucketPolicy.")
    String createbucketPolicyName;

    @CommandLine.Option(names = {"--accessbucketPolicyName"}, description = "Policy name for accessing Katta STS buckets. Defaults to {bucketPrefix}accessbucketpolicy.")
    String accessbucketPolicyName;

    @Override
    public Void call() throws Exception {
        if(createbucketPolicyName == null) {
            createbucketPolicyName = String.format("%screatebucketpolicy", bucketPrefix);
        }
        if(accessbucketPolicyName == null) {
            accessbucketPolicyName = String.format("%saccessbucketpolicy", bucketPrefix);
        }

        final MinioAdminClient minioAdminClient = new MinioAdminClient.Builder()
                .credentials(accessKey, secretKey)
                .endpoint(endpointUrl).build();

        // /mc admin policy create myminio cipherduckcreatebucket /setup/minio_sts/createbucketpolicy.json
        {
            final IamPolicy miniocreatebucketpolicy = IamPolicy.builder()
                    .addStatement(b -> b
                            .effect(IamEffect.ALLOW)
                            .addAction("s3:CreateBucket")
                            .addAction("s3:GetBucketPolicy")
                            .addAction("s3:PutBucketVersioning")
                            .addAction("s3:GetBucketVersioning")
                            .addResource(String.format("arn:aws:s3:::%s*", bucketPrefix)))
                    .addStatement(b -> b
                            .effect(IamEffect.ALLOW)
                            .addAction("s3:PutObject")
                            .addResource(String.format("arn:aws:s3:::%s*/*/", bucketPrefix))
                            .addResource(String.format("arn:aws:s3:::%s*/*.uvf", bucketPrefix)))
                    .build();
            minioAdminClient.addCannedPolicy(createbucketPolicyName, miniocreatebucketpolicy.toJson(IamPolicyWriter.builder()
                    .prettyPrint(true)
                    .build()));
            System.out.println(minioAdminClient.listCannedPolicies().get(createbucketPolicyName));
        }
        // /mc admin policy create myminio cipherduckaccessbucket /setup/minio_sts/accessbucketpolicy.json
        {
            final JSONObject minioaccessbucketpolicy = new JSONObject(IOUtils.toString(KattaSetupCli.class.getResourceAsStream("/setup/local/minio_sts/accessbucketpolicy.json"), Charset.defaultCharset()));
            final JSONArray statements = minioaccessbucketpolicy.getJSONArray("Statement");
            for(int i = 0; i < statements.length(); i++) {
                final List<String> list = statements.getJSONObject(i).getJSONArray("Resource").toList().stream().map(Objects::toString).map(s -> s.replace("katta", bucketPrefix)).toList();
                statements.getJSONObject(i).put("Resource", list);
            }
            minioAdminClient.addCannedPolicy(accessbucketPolicyName, minioaccessbucketpolicy.toString());
            System.out.println(minioAdminClient.listCannedPolicies().get(accessbucketPolicyName));
        }

        final String json = IOUtils.toString(URI.create(hubUrl + "/api/config"), Charset.forName("UTF-8"));
        final JSONObject apiConfig = new JSONObject(json);
        final String wellKnown = String.format("%s/realms/%s/.well-known/openid-configuration", apiConfig.getString("keycloakUrl"), apiConfig.getString("keycloakRealm"));

        String keycloakClientIdCryptomator = apiConfig.getString("keycloakClientIdCryptomator");
        String keycloakClientIdHub = apiConfig.getString("keycloakClientIdHub");
        String keycloakClientIdCryptomatorVaults = apiConfig.getString("keycloakClientIdCryptomatorVaults");
        System.out.println(String.format("""
                        # The MinIO Client API is incomplete (https://github.com/minio/minio/issues/16151).
                        # Please execute the following commands on the command line.
                        # Further info: https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#minio

                        mc alias set myminio %s %s %s

                        mc idp openid add myminio %s \\
                            config_url="%s" \\
                            client_id="%s" \\
                            client_secret="ignore-me" \\
                            role_policy="%s"
                        mc idp openid add myminio %s \\
                            config_url="%s" \\
                            client_id="%s" \\
                            client_secret="ignore-me" \\
                            role_policy="%s"   \s
                        mc idp openid add myminio %s \\
                            config_url="%s" \\
                            client_id="%s" \\
                            client_secret="ignore-me" \\
                            role_policy="%s"   \s
                        mc admin service restart myminio
                        """, endpointUrl, accessKey, secretKey,
                keycloakClientIdCryptomator, wellKnown, keycloakClientIdCryptomator, createbucketPolicyName,
                keycloakClientIdHub, wellKnown, keycloakClientIdHub, accessbucketPolicyName,
                keycloakClientIdCryptomatorVaults, wellKnown, keycloakClientIdCryptomatorVaults, accessbucketPolicyName));
        return null;
    }
}
