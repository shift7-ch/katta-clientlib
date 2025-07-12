/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import java.util.UUID;
import java.util.concurrent.Callable;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import picocli.CommandLine;

/**
 * Archives a storage profile using <code>/api/storageprofile</code>.
 * <p>
 * Requires <code>admin</code> role in Katta Server.
 * <p>
 * See also <a href="https://github.com/shift7-ch/katta-clientlib/blob/main/hub/src/main/resources/openapi.json>OpenAPI Specification</a>.
 */
@CommandLine.Command(name = "storageProfileArchive",
        description = "Upload storage profile for AWS Static.",
        mixinStandardHelpOptions = true)
public class StorageProfileArchive extends AbstractAuthorizationCode implements Callable<Void> {

    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://testing.katta.cloud/tamarind\"", required = true)
    String hubUrl;

    @CommandLine.Option(names = {"--uuid"}, description = "The uuid.", required = true)
    String uuid;

    @Override
    public Void call() throws Exception {
        accessToken = login();
        call(hubUrl, accessToken, uuid);
        return null;
    }

    protected void call(final String hubUrl, final String accessToken, final String uuid) throws ApiException {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hubUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);

        System.out.println("storage profiles:");
        System.out.println(storageProfileResourceApi.apiStorageprofileGet(null));

        storageProfileResourceApi.apiStorageprofileProfileIdPut(UUID.fromString(uuid), true);
        System.out.println("updated:");
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(UUID.fromString(uuid)));
    }
}
