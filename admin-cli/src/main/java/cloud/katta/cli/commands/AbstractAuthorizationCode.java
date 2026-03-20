/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import picocli.CommandLine;

public class AbstractAuthorizationCode {

    @CommandLine.Option(names = {"--tokenUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/realms/cryptomator/protocol/openid-connect/token\"", required = false)
    protected String tokenUrl;

    @CommandLine.Option(names = {"--authUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/realms/cryptomator/protocol/openid-connect/auth\"", required = false)
    protected String authUrl;

    @CommandLine.Option(names = {"--clientId"}, description = "Keycloak realm URL with scheme. Example: \"cryptomator\"", required = false)
    protected String clientId;

    @CommandLine.Option(names = {"--accessToken"}, description = "The access token. If not provided, --tokenUrl, --authUrl and --clientId need to be provided. Requires admin role in the hub.", required = false)
    protected String accessToken;

    public AbstractAuthorizationCode() {
    }

    public AbstractAuthorizationCode(final String tokenUrl, final String authUrl, final String clientId, final String accessToken) {
        this.tokenUrl = tokenUrl;
        this.authUrl = authUrl;
        this.clientId = clientId;
        this.accessToken = accessToken;
    }

    protected String login() throws IOException, InterruptedException {
        if(null == accessToken) {
            if(StringUtils.isEmpty(tokenUrl) || StringUtils.isEmpty(authUrl) || StringUtils.isEmpty(clientId)) {
                throw new IllegalArgumentException("If --accessToken is not provided, you must specify --tokenUrl, --authUrl and --clientId.");
            }
            var authResponse = TinyOAuth2.client(clientId)
                    .withTokenEndpoint(URI.create(tokenUrl))
                    .authorizationCodeGrant(URI.create(authUrl))
                    .authorize(HttpClient.newHttpClient(), uri -> System.out.println("Please login on " + uri));
            return extractAccessToken(authResponse);
        }
        else {
            return accessToken;
        }
    }

    private String extractAccessToken(HttpResponse<String> response) throws IOException {
        var statusCode = response.statusCode();
        if (statusCode != 200) {
            throw new IOException("""
                    Failed to retrieve access token. HTTP status: %d, body:
                    %s
                    """.formatted(statusCode, response.body()));
        }
        var rootNode = new ObjectMapper().reader().readTree(response.body());
        var accessTokenNode = rootNode.get("access_token");
        if (accessTokenNode == null || accessTokenNode.isNull()) {
            throw new IOException("""
                    Failed to parse access token from response body:
                    %s
                    """.formatted(response.body()));
        }
        return accessTokenNode.asText();
    }
}
