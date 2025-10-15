/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Home;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.workflows.CreateVaultService;
import cloud.katta.workflows.DeviceKeysServiceImpl;
import cloud.katta.workflows.UserKeysServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;

public class ManualListTest extends AbstractHubTest {

    @Test
    @Disabled("run manually")
    public void testManualListing() throws Exception {
        final String hubURL = "https://testing.katta.cloud/tamarind";
        final String keycloakAuthServerUrl = "https://testing.katta.cloud/kc/realms/tamarind";
        final String clientId = "cryptomator";
        final String username = System.getenv().get("username");
        final String accountKey = System.getenv().get("account_key");

//        final String storageProfileId = "45a3cd17-9955-4580-9e60-790d84f5785f";
        final String storageProfileId = null;

        final String vaultName = "vault " + username + " " + new Date();
        final String vaultDescription = "fancy";

        String initialAccessToken;
        try (var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())
                .build()) {

            final HttpResponse<String> authResponse = TinyOAuth2.client(clientId)
                    .withTokenEndpoint(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/token"))
                    .authorizationCodeGrant(URI.create(keycloakAuthServerUrl + "/protocol/openid-connect/auth"))
                    .authorize(client, uri -> {
                        System.out.println("Please login on " + uri);
                    });

            Assertions.assertEquals(200, authResponse.statusCode());
            initialAccessToken = new ObjectMapper().reader().readTree(authResponse.body()).get("access_token").asText();
        }

        HubTestConfig.Setup config = new HubTestConfig.Setup().withUserConfig(
                new HubTestConfig.Setup.UserConfig(new Credentials(username, null).withOauth(new OAuthTokens(initialAccessToken, null, System.currentTimeMillis() + 300 * 1000, initialAccessToken)), accountKey)
        ).withHubURL(hubURL);


        final HubSession hubSession = setupConnection(config);
        printVaults(hubSession);


        if(storageProfileId != null) {
            final VaultResourceApi vaults = new VaultResourceApi(hubSession.getClient());

            System.out.println(vaults.apiVaultsAccessibleGet(null));
            List<StorageProfileDto> storageProfileDtos = new StorageProfileResourceApi(hubSession.getClient()).apiStorageprofileGet(false);
            for(StorageProfileDto storageProfileDto : storageProfileDtos) {
                System.out.println(storageProfileDto);
            }
            final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost()));

            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(hubSession.getClient()).apiStorageprofileGet(false);

            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(storageProfileId)).findFirst().get();
            final UUID vaultId = UUID.randomUUID();

            new CreateVaultService(hubSession).createVault(userKeys, storageProfileWrapper, new CreateVaultService.CreateVaultModel(
                    vaultId, vaultName, vaultDescription,
                    storageProfileId, null, null, null, "eu-west-1", true, 3));

            printVaults(hubSession);
        }


    }

    private static void printVaults(HubSession hubSession) throws ApiException, BackgroundException {
        final AttributedList<Path> vaults = hubSession.getFeature(ListService.class).list(Home.root(), new DisabledListProgressListener());
        final List<VaultDto> vaultL = new VaultResourceApi(hubSession.getClient()).apiVaultsAccessibleGet(null);
        for(VaultDto vaultDto : vaultL) {
            if(vaultDto.getArchived()) {
                continue;
            }
            final Path bucket = new Path("katta" + vaultDto.getId(), EnumSet.of(Path.Type.volume, Path.Type.directory));
            final Path vault = vaults.find(new SimplePathPredicate(bucket));
            {
                // decrypted file listing
                try {
                    final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                    System.out.printf("success on %s (%s) %n", vault, vaultDto.getName());
                    System.out.println("==================>>>");
                    for(Path path : list) {
                        System.out.println(path);
                    }
                    System.out.println("===================<<<");
                }
                catch(IllegalArgumentException e) {
                    System.out.printf("failed on %s (%s): %s %n", vault, vaultDto.getName(), vaultDto);
                }
            }
        }
    }
}
