package cloud.katta.cli.commands;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import picocli.CommandLine;

// https://github.com/cryptomator/hub-cli/commit/bffcf2805530976c4a758990958ff75f9df68c0e#diff-c349f933a7698e31cfe25bd0a638ae487a02ac6fcb429bcce3e315aa8832be8b
@CommandLine.Command(name = "authorizationCode", description = "Get token using authorization code flow.", mixinStandardHelpOptions = true)
public class AuthorizationCode implements Callable<Void> {

    @CommandLine.Option(names = {"--tokenUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/token\"", required = true)
    String tokenUrl;

    @CommandLine.Option(names = {"--authUrl"}, description = "Keycloak realm URL with scheme. Example: \"https://testing.katta.cloud/kc/realms/tamarind/protocol/openid-connect/auth\"", required = true)
    String authUrl;

    @CommandLine.Option(names = {"--clientId"}, description = "Keycloak realm URL with scheme. Example: \"cryptomator\"", required = true)
    String clientId;


    @Override
    public Void call() throws Exception {
        var authResponse = TinyOAuth2.client(clientId)
                .withTokenEndpoint(URI.create(tokenUrl))
                .authorizationCodeGrant(URI.create(authUrl))
                .authorize(HttpClient.newHttpClient(), uri -> {
                    System.out.println("Please login on " + uri);
                });
        System.out.println(authResponse);
        printAccessToken(authResponse);
        return null;
    }

    private static int printAccessToken(HttpResponse<String> response) throws JsonProcessingException {
        var statusCode = response.statusCode();
        if(statusCode != 200) {
            System.err.println("""
                    Request was responded with code %d and body:\n%s\n""".formatted(statusCode, response.body()));
            return statusCode;
        }
        var token = new ObjectMapper().reader().readTree(response.body()).get("access_token").asText();
        System.out.println(token);
        return 0;
    }
}
