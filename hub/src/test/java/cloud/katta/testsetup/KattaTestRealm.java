/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.cyberduck.core.PreferencesUseragentProvider;
import ch.cyberduck.core.http.UserAgentHttpRequestInitializer;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Properties;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.CreateUserDto;
import cloud.katta.client.model.RealmRole;
import cloud.katta.client.model.UpdateUserDto;
import cloud.katta.client.model.WithCounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Adds the test configuration to the realm of <a href="https://github.com/shift7-ch/katta-compose">katta-compose</a>, which is
 * rendered from the Helm chart of Katta Server and contains no test users.
 */
public final class KattaTestRealm {
    private static final Logger log = LogManager.getLogger(KattaTestRealm.class.getName());

    /**
     * Keycloak administrator of the master realm in katta-compose
     */
    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";

    private static final ObjectMapper mapper = new ObjectMapper();

    private KattaTestRealm() {
    }

    /**
     * Enable direct access grants for the password grant of the tests and create the test user with Katta Server,
     * which adds it to Keycloak and its database at once.
     *
     * @param env   Variables of the env file passed to katta-compose
     * @param setup Test setup with the admin and user credentials
     */
    public static void setup(final Properties env, final HubTestConfig.Setup setup) throws IOException, ApiException {
        final String keycloakUrl = env.getProperty("HUB_KEYCLOAK_URL") + env.getProperty("HUB_KEYCLOAK_BASEPATH", "");
        enableDirectAccessGrants(keycloakUrl, env.getProperty("HUB_KEYCLOAK_REALM"), setup.clientId);
        final ApiClient adminApiClient = HubTestUtilities.getAdminApiClient(setup);
        createUser(adminApiClient, setup.userConfig);
        if(setup.memberConfig != null) {
            createUser(adminApiClient, setup.memberConfig);
        }
    }

    private static void enableDirectAccessGrants(final String keycloakUrl, final String realm, final String clientId) throws IOException {
        final ApacheHttpTransport transport = new ApacheHttpTransport();
        final String token = new PasswordTokenRequest(transport, new GsonFactory(),
                new GenericUrl(String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl)), KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASSWORD)
                .setClientAuthentication(new ClientParametersAuthentication("admin-cli", null))
                .setRequestInitializer(new UserAgentHttpRequestInitializer(new PreferencesUseragentProvider()))
                .executeUnparsed().parseAs(OAuth2AuthorizationService.PermissiveTokenResponse.class).toTokenResponse().getAccessToken();
        final HttpRequestFactory requests = transport.createRequestFactory(request ->
                request.setHeaders(new HttpHeaders().setAuthorization(String.format("Bearer %s", token))));
        final GenericUrl clients = new GenericUrl(String.format("%s/admin/realms/%s/clients", keycloakUrl, realm));
        clients.set("clientId", clientId);
        final JsonNode representations = mapper.readTree(requests.buildGetRequest(clients).execute().parseAsString());
        final ObjectNode representation = (ObjectNode) representations.get(0);
        representation.put("directAccessGrantsEnabled", true);
        requests.buildPutRequest(new GenericUrl(String.format("%s/admin/realms/%s/clients/%s", keycloakUrl, realm, representation.get("id").asText())),
                new ByteArrayContent("application/json", mapper.writeValueAsString(representation).getBytes(StandardCharsets.UTF_8))).execute();
        log.info("Enabled direct access grants for client {}", clientId);
    }

    private static void createUser(final ApiClient adminApiClient, final HubTestConfig.Setup.UserConfig user) throws ApiException {
        final UsersResourceApi users = new UsersResourceApi(adminApiClient);
        final EnumSet<RealmRole> roles = EnumSet.of(RealmRole.USER, RealmRole.CREATE_VAULTS);
        final String email = String.format("%s@localhost", user.username);
        if(!find(users, user.username).isPresent()) {
            users.apiUsersPost(new CreateUserDto().name(user.username).email(email).firstName(user.username).lastName(user.username)
                    .password(user.password).realmRoles(roles));
            log.info("Created user {}", user.username);
        }
        // Katta Server creates users with a temporary password, which requires a password change before any login. Reset
        // password and roles of an existing user as well when the setup runs against an environment already set up.
        final String id = find(users, user.username).orElseThrow(() -> new IllegalStateException(String.format("User %s not found", user.username))).getId();
        users.apiUsersIdPut(id, new UpdateUserDto().email(email).firstName(user.username).lastName(user.username)
                .password(user.password).realmRoles(roles));
        log.info("Set password and roles of user {} with id {}", user.username, id);
    }

    private static Optional<WithCounts> find(final UsersResourceApi users, final String username) throws ApiException {
        return users.apiUsersGet().stream().filter(u -> username.equals(u.getName())).findFirst();
    }
}
