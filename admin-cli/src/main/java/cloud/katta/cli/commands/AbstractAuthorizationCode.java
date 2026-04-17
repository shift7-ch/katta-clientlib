/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import ch.cyberduck.core.Factory;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import picocli.CommandLine;

public class AbstractAuthorizationCode {

    @CommandLine.Option(names = {"--hubUrl"}, description = "Hub URL. Example: \"https://hub.default.katta.cloud/\"", required = true)
    protected String hubUrl;

    @CommandLine.Option(names = {"--tokenUrl"}, description = "Keycloak token endpoint URL. Fetched from --hubUrl if not provided.", required = false)
    protected String tokenUrl;

    @CommandLine.Option(names = {"--authUrl"}, description = "Keycloak auth endpoint URL. Fetched from --hubUrl if not provided.", required = false)
    protected String authUrl;

    @CommandLine.Option(names = {"--clientId"}, description = "Client ID to authorize with. Example: \"cryptomator\"", required = false, defaultValue = "cryptomator")
    protected String clientId;

    @CommandLine.Option(names = {"--accessToken"}, description = "The access token. If not provided, --hubUrl (or --tokenUrl and --authUrl) and --clientId need to be provided. Requires admin role in the hub.", required = false)
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
            if(StringUtils.isEmpty(tokenUrl) || StringUtils.isEmpty(authUrl)) {
                if(StringUtils.isEmpty(hubUrl)) {
                    throw new IllegalArgumentException("If --accessToken is not provided, you must specify --hubUrl (or --tokenUrl and --authUrl).");
                }
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(hubUrl + "/api/config"))
                        .GET()
                        .build();
                try (HttpClient client = HttpClient.newHttpClient()) {
                    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if(response.statusCode() != 200) {
                        throw new IOException("Failed to fetch config from %s/api/config. HTTP status: %d".formatted(hubUrl, response.statusCode()));
                    }
                    var rootNode = new ObjectMapper().reader().readTree(response.body());
                    this.tokenUrl = rootNode.get("keycloakTokenEndpoint").asText();
                    this.authUrl = rootNode.get("keycloakAuthEndpoint").asText();
                }
            }
            var authResponse = TinyOAuth2.client(clientId)
                    .withTokenEndpoint(URI.create(tokenUrl))
                    .authorizationCodeGrant(URI.create(authUrl))
                    .authorize(HttpClient.newHttpClient(), uri -> {
                        System.out.println("Please login on " + uri);
                        this.open(uri);
                    });
            return extractAccessToken(authResponse);
        }
        else {
            return accessToken;
        }
    }

    private boolean open(final URI uri) {
        String[] command;
        switch(Factory.Platform.getDefault()) {
            case mac:
                command = new String[]{"open", uri.toString()};
                break;
            case windows:
                command = new String[]{"cmd", "/c", "start", uri.toString()};
                break;
            case linux:
                command = new String[]{"xdg-open", uri.toString()};
                break;
            default:
                return false;
        }
        try {
            return new ProcessBuilder(command).start().exitValue() == 0;
        }
        catch(IOException e) {
            return false;
        }
    }

    private String extractAccessToken(HttpResponse<String> response) throws IOException {
        var statusCode = response.statusCode();
        if(statusCode != 200) {
            throw new IOException("""
                    Failed to retrieve access token. HTTP status: %d, body:
                    %s
                    """.formatted(statusCode, response.body()));
        }
        var rootNode = new ObjectMapper().reader().readTree(response.body());
        var accessTokenNode = rootNode.get("access_token");
        if(accessTokenNode == null || accessTokenNode.isNull()) {
            throw new IOException("""
                    Failed to parse access token from response body:
                    %s
                    """.formatted(response.body()));
        }
        return accessTokenNode.asText();
    }
}
