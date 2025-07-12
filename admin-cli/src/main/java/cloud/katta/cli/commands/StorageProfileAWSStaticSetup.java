/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileS3Dto;
import picocli.CommandLine;

/**
 * Uploads a storage profile to Katta Server for use with AWS static.
 * The storage profile then allows users with <code>create-vaults</code> role to create vaults for an existing AWS S3 bucket.
 * <p>
 * See also: <a href="https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md#storage-profiles">katta docs</a>.
 */
@CommandLine.Command(name = "storageProfileAWSStatic",
        description = "Upload storage profile for AWS Static.",
        mixinStandardHelpOptions = true)
public class StorageProfileAWSStaticSetup extends AbstractAuthorizationCode implements Callable<Void> {
    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://testing.katta.cloud/tamarind\"", required = true)
    String hubUrl;


    @Override
    public Void call() throws Exception {
        final UUID uuid = UUID.randomUUID();
        accessToken = login();
        call(hubUrl, accessToken, uuid);
        return null;
    }

    protected void call(final String hubUrl, final String accessToken, final UUID uuid) throws IOException, ApiException {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hubUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);

        storageProfileResourceApi.apiStorageprofileS3Put(new StorageProfileS3Dto()
                .id(uuid)
                .name("AWS S3 static")
                .protocol(Protocol.S3)
                .storageClass(S3STORAGECLASSES.STANDARD)
                .archived(false)
                .scheme("https")
                .port(443)
        );
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }
}
