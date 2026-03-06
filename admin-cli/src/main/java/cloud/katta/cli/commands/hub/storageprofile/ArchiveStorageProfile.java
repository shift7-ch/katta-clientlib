/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import java.util.UUID;
import java.util.concurrent.Callable;

import cloud.katta.cli.commands.AbstractAuthorizationCode;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import picocli.CommandLine;

/**
 * Archives a storage profile using <code>/api/storageprofile</code>.
 * <p>
 * Requires <code>admin</code> role in Katta Server.
 * <p>
 * See also <a href="https://github.com/shift7-ch/katta-clientlib/blob/main/hub/src/main/resources/openapi.json">OpenAPI Specification</a>.
 */
@CommandLine.Command(name = "archive",
        description = "Archive (deactivate) an existing storage profile.",
        mixinStandardHelpOptions = true)
public class ArchiveStorageProfile extends AbstractAuthorizationCode implements Callable<Void> {
    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://hub.testing.katta.cloud\"", required = true)
    String hubUrl;

    @CommandLine.Option(names = {"--uuid"}, description = "The uuid.", required = true)
    String uuid;

    public ArchiveStorageProfile() {
    }

    public ArchiveStorageProfile(final String tokenUrl, final String authUrl, final String clientId, final String accessToken, final String hubUrl, final String uuid) {
        super(tokenUrl, authUrl, clientId, accessToken);
        this.hubUrl = hubUrl;
        this.uuid = uuid;
    }

    @Override
    public Void call() throws Exception {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hubUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer %s".formatted(this.login()));
        this.call(new StorageProfileResourceApi(apiClient));
        return null;
    }

    protected void call(final StorageProfileResourceApi storageProfileResourceApi) throws ApiException {
        System.out.println("storage profiles:");
        System.out.println(storageProfileResourceApi.apiStorageprofileGet(null));
        storageProfileResourceApi.apiStorageprofileProfileIdPut(UUID.fromString(uuid), true);
        System.out.println("updated:");
        System.out.println(storageProfileResourceApi.apiStorageprofileProfileIdGet(UUID.fromString(uuid)));
    }
}
