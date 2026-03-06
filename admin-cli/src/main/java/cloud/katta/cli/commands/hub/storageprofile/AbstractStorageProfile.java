/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import java.util.UUID;
import java.util.concurrent.Callable;

import cloud.katta.cli.commands.AbstractAuthorizationCode;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import picocli.CommandLine;

public abstract class AbstractStorageProfile extends AbstractAuthorizationCode implements Callable<Void> {
    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://hub.default.katta.cloud/\"", required = true)
    String hubUrl;

    @CommandLine.Option(names = {"--uuid"}, description = "The uuid.", required = false)
    String uuid;

    @CommandLine.Option(names = {"--name"}, description = "The name.", required = false, defaultValue = "Storage Profile")
    String name;

    @Override
    public Void call() throws Exception {
        final String accessToken = login();
        call(hubUrl, accessToken, null == uuid ? UUID.randomUUID().toString() : uuid, name);
        return null;
    }

    protected void call(final String hubUrl, final String accessToken, final String uuid, final String name) throws ApiException {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hubUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        call(UUID.fromString(uuid), name, apiClient);
    }

    protected abstract void call(final UUID uuid, final String name, final ApiClient apiClient) throws ApiException;
}
