/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.hub.storageprofile;

import java.util.List;
import java.util.concurrent.Callable;

import cloud.katta.cli.commands.AbstractAuthorizationCode;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.JSON;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.StorageProfileDto;
import picocli.CommandLine;

public abstract class AbstractStorageProfile extends AbstractAuthorizationCode implements Callable<Void> {
    @CommandLine.Option(names = {"--uuid"}, description = "The uuid.", required = false)
    protected String uuid;

    @CommandLine.Option(names = {"--name"}, description = "The name.", required = false)
    protected String name;

    @CommandLine.Option(names = {"--region"}, description = "Default Bucket region, e.g. \"eu-west-1\".", required = true)
    protected String region;

    @CommandLine.Option(names = {"--regions"}, description = "Bucket regions, e.g. \"--regions eu-west-1  --regions eu-west-2 --regions eu-west-3\".", required = false)
    protected List<String> regions;

    @CommandLine.Option(names = {"--debug"}, description = "Print HTTP request and response headers.", defaultValue = "false")
    protected boolean debug;

    public AbstractStorageProfile() {
    }

    public AbstractStorageProfile(final String hubUrl, final String uuid, final String name, final String region, final List<String> regions) {
        this.hubUrl = hubUrl;
        this.uuid = uuid;
        this.name = name;
        this.region = region;
        this.regions = regions;
    }

    @Override
    public Void call() throws Exception {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hubUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer %s".formatted(this.login()));
        apiClient.setDebugging(debug);
        final StorageProfileDto response = this.call(new StorageProfileResourceApi(apiClient));
        System.out.println(new JSON().getContext(null).writeValueAsString(response));
        return null;
    }

    protected abstract StorageProfileDto call(final StorageProfileResourceApi storageProfileResourceApi) throws ApiException;
}
