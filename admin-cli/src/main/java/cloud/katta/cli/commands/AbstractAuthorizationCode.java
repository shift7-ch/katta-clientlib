/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import picocli.CommandLine;

public class AbstractAuthorizationCode {

    @CommandLine.Option(names = {"--tokenUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token\"", required = false)
    String tokenUrl;

    @CommandLine.Option(names = {"--authUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth\"", required = false)
    String authUrl;

    @CommandLine.Option(names = {"--clientId"}, description = "Keycloak realm URL with scheme. Example: \"cryptomator\"", required = false)
    String clientId;

    @CommandLine.Option(names = {"--accessToken"}, description = "The access token. If not provided, --tokenUrl, --authUrl and --clientId need to be provided. Requires admin role in the hub.", required = false)
    String accessToken;

    protected String login() throws IOException, InterruptedException {
        if(StringUtils.isEmpty(accessToken)) {
            var authResponse = TinyOAuth2.client(clientId)
                    .withTokenEndpoint(URI.create(tokenUrl))
                    .authorizationCodeGrant(URI.create(authUrl))
                    .authorize(HttpClient.newHttpClient(), uri -> {
                        System.out.println("Please login on " + uri);
                    });
            return extractAccessToken(authResponse);
        }
        else {
            return accessToken;
        }
    }

    private static String extractAccessToken(HttpResponse<String> response) throws JsonProcessingException {
        var statusCode = response.statusCode();
        if(statusCode != 200) {
            System.err.println("""
                    Request was responded with code %d and body:\n%s\n""".formatted(statusCode, response.body()));
            return null;
        }
        return new ObjectMapper().reader().readTree(response.body()).get("access_token").asText();
    }
}
