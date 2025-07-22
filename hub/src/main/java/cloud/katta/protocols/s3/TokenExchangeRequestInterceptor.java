/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.preferences.PreferencesReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageResourceApi;
import cloud.katta.client.auth.HttpBearerAuth;
import cloud.katta.client.model.AccessTokenResponse;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

import static cloud.katta.protocols.s3.S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE;

/**
 * Exchange OIDC token to scoped token using OAuth 2.0 Token Exchange. Used for S3-STS in Katta.
 */
public class TokenExchangeRequestInterceptor extends OAuth2RequestInterceptor {
    private static final Logger log = LogManager.getLogger(TokenExchangeRequestInterceptor.class);

    // https://openid.net/specs/openid-connect-core-1_0.html
    public static final String OIDC_AUTHORIZED_PARTY = "azp";


    private final Host bookmark;
    private final HttpClient client;

    public TokenExchangeRequestInterceptor(final HttpClient client, final Host bookmark, final LoginCallback prompt) throws LoginCanceledException {
        super(client, bookmark, prompt);
        this.bookmark = bookmark;
        this.client = client;
    }

    @Override
    public OAuthTokens authorize() throws BackgroundException {
        return this.exchange(super.authorize());
    }

    @Override
    public OAuthTokens refresh(final OAuthTokens previous) throws BackgroundException {
        return this.exchange(super.refresh(previous));
    }

    /**
     * Perform OAuth 2.0 Token Exchange
     *
     * @param previous Input tokens retrieved to exchange at the token endpoint
     * @return New tokens
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_CLIENT_ID
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES
     */
    public OAuthTokens exchange(final OAuthTokens previous) throws BackgroundException {
        log.info("Exchange tokens {} for {}", previous, bookmark);
        final PreferencesReader preferences = new HostPreferences(bookmark);
        final ApiClient apiClient = new ApiClient(Collections.singletonMap("bearer", new HttpBearerAuth("bearer")));
        apiClient.addDefaultHeader("Authorization",String.format("Bearer %s", previous.getAccessToken()));
        apiClient.setBasePath("http://localhost:8280/");

        final StorageResourceApi api = new StorageResourceApi(apiClient);
        try {
            AccessTokenResponse tokenExchangeResponse = api.apiStorageS3TokenPost(preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES));
            // N.B. token exchange with Id token does not work!
            final OAuthTokens tokens = new OAuthTokens(tokenExchangeResponse.getAccessToken(),
                    tokenExchangeResponse.getRefreshToken(),
                    System.currentTimeMillis() + tokenExchangeResponse.getExpiresIn() * 1000);
            log.debug("Received exchanged token {} for {}", tokens, bookmark);
            return tokens;
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    @Override
    public Credentials validate() throws BackgroundException {
        final Credentials credentials = super.validate();
        final OAuthTokens tokens = credentials.getOauth();
        final String accessToken = tokens.getAccessToken();
        final PreferencesReader preferences = new HostPreferences(bookmark);
        final String tokenExchangeClientId = preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_CLIENT_ID);
        if(StringUtils.isEmpty(tokenExchangeClientId)) {
            log.warn("Found {} empty, although {} is set to {} - misconfiguration?", S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_CLIENT_ID, OAUTH_TOKENEXCHANGE, preferences.getBoolean(OAUTH_TOKENEXCHANGE));
            return credentials;
        }
        try {
            final DecodedJWT jwt = JWT.decode(accessToken);

            final List<String> auds = jwt.getAudience();
            final String azp = jwt.getClaim(OIDC_AUTHORIZED_PARTY).asString();

            final boolean audNotUnique = 1 != auds.size(); // either multiple audiences or none
            // do exchange if aud is not unique or azp is not equal to aud
            if(audNotUnique || !auds.get(0).equals(azp)) {
                log.debug("None or multiple audiences found {} or audience differs from azp {}, triggering token-exchange.", Arrays.toString(auds.toArray()), azp);
                return credentials.withOauth(this.exchange(tokens));
            }
        }
        catch(JWTDecodeException e) {
            throw new LoginFailureException("Invalid JWT or JSON format in authentication token", e);
        }
        return credentials;
    }
}
