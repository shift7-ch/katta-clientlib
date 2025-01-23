/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

import ch.cyberduck.core.PreferencesUseragentProvider;
import ch.cyberduck.core.http.UserAgentHttpRequestInitializer;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.Pair;
import ch.iterate.hub.client.api.ConfigResourceApi;
import ch.iterate.hub.client.model.ConfigDto;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class HubTestUtilities {

    public static ApiClient getAdminApiClient(final HubTestConfig.Setup setup) throws IOException, ApiException {
        final ConfigDto config = new ConfigResourceApi(new ApiClient().setBasePath(setup.hubURL)).apiConfigGet();
        final PasswordTokenRequest request = new PasswordTokenRequest(new ApacheHttpTransport(), new GsonFactory(), new GenericUrl(config.getKeycloakTokenEndpoint()),
                setup.adminConfig.username, setup.adminConfig.password)
                .setClientAuthentication(new ClientParametersAuthentication(setup.clientId, null))
                .setRequestInitializer(new UserAgentHttpRequestInitializer(new PreferencesUseragentProvider()));
        final String adminAccessToken = request.executeUnparsed().parseAs(OAuth2AuthorizationService.PermissiveTokenResponse.class).toTokenResponse().getAccessToken();
        final ApiClient adminApiClient = new ApiClient() {
            @Override
            protected void updateParamsForAuth(final String[] authNames, final List<Pair> queryParams, final Map<String, String> headerParams, final Map<String, String> cookieParams, final String payload, final String method, final URI uri) throws ApiException {
                super.updateParamsForAuth(authNames, queryParams, headerParams, cookieParams, payload, method, uri);
                headerParams.put("Authorization", String.format("Bearer %s", adminAccessToken));
            }
        };
        return adminApiClient.setBasePath(setup.hubURL);
    }
}
