/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import java.util.UUID;

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
public class StorageProfileArchive extends AbstractStorageProfile {

    @Override
    protected void call(final UUID uuid, final String name, final ApiClient apiClient) throws ApiException {
        final StorageProfileResourceApi storageProfileResourceApi = new StorageProfileResourceApi(apiClient);
        call(uuid, storageProfileResourceApi);
    }

    protected void call(final UUID uuid, final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        System.out.println("storage profiles:");
        System.out.println(storageProfileResourceApi.apiStorageprofileGet(null));
        storageProfileResourceApi.apiStorageprofileProfileIdPut(uuid, true);
        System.out.println("updated:");
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(uuid));
    }
}
