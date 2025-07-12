/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub;

import java.util.UUID;
import java.util.concurrent.Callable;

import cloud.katta.cli.commands.AbstractAuthorizationCode;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import picocli.CommandLine;

public abstract class AbstractStorageProfile extends AbstractAuthorizationCode implements Callable<Void> {
    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://testing.katta.cloud/tamarind\"", required = true)
    String hubUrl;

    @CommandLine.Option(names = {"--uuid"}, description = "The uuid.", required = true)
    String uuid;

    @Override
    public Void call() throws Exception {
        final String accessToken = login();
        call(hubUrl, accessToken, uuid);
        return null;
    }

    protected void call(final String hubUrl, final String accessToken, final String uuid) throws ApiException {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hubUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        call(UUID.fromString(uuid), apiClient);
    }


    protected abstract void call(final UUID uuid, final ApiClient apiClient) throws ApiException;
}
