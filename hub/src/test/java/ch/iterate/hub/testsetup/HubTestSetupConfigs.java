/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import ch.cyberduck.test.VaultTest;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.iterate.hub.testsetup.model.HubTestConfig;
import ch.iterate.hub.testsetup.model.HubTestSetupConfig;
import ch.iterate.hub.testsetup.model.HubTestSetupUserConfig;
import ch.iterate.hub.testsetup.model.VaultSpec;

public class HubTestSetupConfigs extends VaultTest {

    static {
        credentials();
    }

    private static final VaultSpec minioSTSVaultConfig = new VaultSpec("MinIO STS", "732D43FA-3716-46C4-B931-66EA5405EF1C", null, null, null);
    private static final VaultSpec minioStaticVaultConfig = new VaultSpec("MinIO static", "71B910E0-2ECC-46DE-A871-8DB28549677E", "handmade", "minioadmin", "minioadmin");
    private static final VaultSpec awsSTSVaultConfig = new VaultSpec("AWS STS", "844BD517-96D4-4787-BCFA-238E103149F6", null, null, null);

    private static final VaultSpec awsStaticVaultConfig = new VaultSpec("AWS static", "72736C19-283C-49D3-80A5-AB74B5202543", "cipherststest",
            PROPERTIES.get(String.format("%s.user", "cipherduck.AWS_CIPHERSTSTEST")),
            PROPERTIES.get(String.format("%s.password", "cipherduck.AWS_CIPHERSTSTEST"))
    );


    private static String staticSetupCode() {
        // set to some value to have staticSetupCode instead of random string in attended local test setup.
        return "blabla";
    }

    /**
     * Unattended local only: hub, Keycloak, MinIO started via docker-compose.
     */
    public static HubTestSetupConfig UNATTENDED_LOCAL_ONLY = new HubTestSetupConfig()
            .withHubURL("http://localhost:8280")
            .withUSER(new HubTestSetupUserConfig("alice", "asd", (staticSetupCode() == null) ? UUID.randomUUID().toString() : staticSetupCode()))
            .withADMIN(new HubTestSetupUserConfig("admin", "admin", (staticSetupCode() == null) ? UUID.randomUUID().toString() : staticSetupCode()))
            .withDockerConfig(new HubTestSetupConfig.DockerConfig("/docker-compose-minio-localhost-hub.yml", 8380, 9100, 9101, 8280));
    private static final Function<VaultSpec, Arguments> argumentUnattendedLocalOnly = vs -> Arguments.of(Named.of(String.format("%s %s (%s) %s", UNATTENDED_LOCAL_ONLY.hubURL(), vs.storageProfileName, vs.storageProfileId, vs.bucketName), new HubTestConfig(UNATTENDED_LOCAL_ONLY, vs)));
    public static Arguments minioSTSUnattendedLocalOnly = argumentUnattendedLocalOnly.apply(minioSTSVaultConfig);
    public static Arguments minioStaticUnattendedLocalOnly = argumentUnattendedLocalOnly.apply(minioStaticVaultConfig);

    /**
     * Unattended Keycloak Testing: local hub (docker-compose) against AWS/MinIO/Keycloak remote
     */

    public static HubTestSetupConfig UNATTENDED_LOCAL_KEYCLOAK_TESTING = new HubTestSetupConfig()
            // N.B. port needs to match dev-realm.json as injected by hub/pom.xml
            .withHubURL("http://localhost:8280")
            .withUSER(new HubTestSetupUserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    "blabla"))
            .withADMIN(new HubTestSetupUserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    "blabla"))
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 improvement: no need to start keycloak in this setting
            .withDockerConfig(new HubTestSetupConfig.DockerConfig("/docker-compose-minio-localhost-hub.yml", 8380, 9100, 9101, 8280));
    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 implement unattended against deployed Keycloak including AWS incl. cleanup and ci and authorizationCode!
// aws s3 ls | grep cipherduck | sort --reverse | awk '{print $3}'
//    aws s3 rm s3://$1 --recursive; aws s3 rb s3://$1 --force
// https://towardsthecloud.com/aws-cli-empty-s3-bucket
// aws s3api delete-objects --bucket $1  --delete "$(aws s3api list-object-versions  --bucket $1 --output=json --query='{Objects: Versions[].{Key:Key,VersionId:VersionId}}')"; aws s3 rb s3://$1 --force

    /**
     * Attended local only: hub, Keycloak, minio started manually.
     */
    private static final HubTestSetupConfig ATTENDED_LOCAL_ONLY = new HubTestSetupConfig()
            .withHubURL("http://localhost:8080")
            .withADMIN(new HubTestSetupUserConfig("admin", "admin", "blabla"))
            .withUSER(new HubTestSetupUserConfig("alice", "asd", "blabla"));
    private static final Function<VaultSpec, Arguments> argumentAttendedLocalOnly = vs -> Arguments.of(Named.of(String.format("%s %s (%s) %s", ATTENDED_LOCAL_ONLY.hubURL(), vs.storageProfileName, vs.storageProfileId, vs.bucketName), new HubTestConfig(ATTENDED_LOCAL_ONLY, vs)));
    public static final Arguments minioSTSAttendedLocalOnly = argumentAttendedLocalOnly.apply(minioSTSVaultConfig);
    public static final Arguments minioStaticAttendedLocalOnly = argumentAttendedLocalOnly.apply(minioStaticVaultConfig);

    // TODO remove
    public static Stream<Arguments> provideAttendedLocalOnlyStatic() {
        return Stream.of(minioStaticAttendedLocalOnly);
    }

    public static Stream<Arguments> provideAttendedLocalOnlySTS() {
        return Stream.of(minioSTSAttendedLocalOnly);
    }

    /**
     * Attended Keycloak Testing: local hub, local MinIO, remote AWS, remote Keycloak.
     */
    public static HubTestSetupConfig ATTENDED_LOCAL_KEYCLOAK_TESTING = new HubTestSetupConfig()
            .withHubURL("http://localhost:8080")
            .withUSER(new HubTestSetupUserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_USER001")),
                    "blabla"))
            .withADMIN(new HubTestSetupUserConfig(
                    PROPERTIES.get(String.format("%s.user", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    PROPERTIES.get(String.format("%s.password", "cipherduck.TESTING_CRYPTOMATOR_ADMIN")),
                    "blabla"));
    private static final Function<VaultSpec, Arguments> argumentAttendedLocalKeycloadkDev = vs -> Arguments.of(Named.of(String.format("%s %s (%s) %s", ATTENDED_LOCAL_KEYCLOAK_TESTING.hubURL(), vs.storageProfileName, vs.storageProfileId, vs.bucketName), new HubTestConfig(ATTENDED_LOCAL_KEYCLOAK_TESTING, vs)));
    public static Arguments minioSTSAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(minioSTSVaultConfig);
    public static Arguments minioStaticAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(minioStaticVaultConfig);
    public static Arguments awsSTSAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(awsSTSVaultConfig);
    public static Arguments awsStaticAttendedLocalKeycloadkDev = argumentAttendedLocalKeycloadkDev.apply(awsStaticVaultConfig);
}
