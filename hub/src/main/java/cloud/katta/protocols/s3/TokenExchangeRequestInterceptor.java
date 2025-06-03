/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.http.DefaultHttpResponseExceptionMappingService;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.oauth.OAuthExceptionMappingService;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.preferences.PreferencesReader;

import com.auth0.jwt.RegisteredClaims;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Exchange OIDC token to scoped token using OAuth 2.0 Token Exchange
 */
public class TokenExchangeRequestInterceptor extends OAuth2RequestInterceptor {
    private static final Logger log = LogManager.getLogger(TokenExchangeRequestInterceptor.class);

    // https://datatracker.ietf.org/doc/html/rfc8693#name-request
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID = "client_id";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_SECRET = "client_secret";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_AUDIENCE = "audience";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN = "subject_token";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN_TYPE = "subject_token_type";
    public static final String OAUTH_TOKEN_TOKEN_TYPE_ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";
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
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_AUDIENCE
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES
     */
    public OAuthTokens exchange(final OAuthTokens previous) throws BackgroundException {
        log.info("Exchange tokens {} for {}", previous, bookmark);
        final TokenRequest request = new TokenRequest(
                new ApacheHttpTransport(client),
                new GsonFactory(),
                new GenericUrl(bookmark.getProtocol().getOAuthTokenUrl()),
                OAUTH_GRANT_TYPE_TOKEN_EXCHANGE
        );
        request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID, bookmark.getProtocol().getOAuthClientId());
        final PreferencesReader preferences = new HostPreferences(bookmark);
        if(!StringUtils.isEmpty(preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE))) {
            request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID, preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE));
            request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_SECRET, preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE_CLIENT_SECRET));
        }
        request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN, previous.getAccessToken());
        request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN_TYPE, OAUTH_TOKEN_TOKEN_TYPE_ACCESS_TOKEN);
        final ArrayList<String> scopes = new ArrayList<>(bookmark.getProtocol().getOAuthScopes());
        if(!StringUtils.isEmpty(preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES))) {
            scopes.addAll(Arrays.asList(preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES).split(" ")));
        }
        request.setScopes(scopes);
        log.debug("Token exchange request {} for {}", request, bookmark);
        try {
            final TokenResponse tokenExchangeResponse = request.execute();
            // N.B. token exchange with Id token does not work!
            final OAuthTokens tokens = new OAuthTokens(tokenExchangeResponse.getAccessToken(),
                    tokenExchangeResponse.getRefreshToken(),
                    System.currentTimeMillis() + tokenExchangeResponse.getExpiresInSeconds() * 1000);
            log.debug("Received exchanged token {} for {}", tokens, bookmark);
            return tokens;
        }
        catch(TokenResponseException e) {
            throw new OAuthExceptionMappingService().map(e);
        }
        catch(HttpResponseException e) {
            throw new DefaultHttpResponseExceptionMappingService().map(new org.apache.http.client
                    .HttpResponseException(e.getStatusCode(), e.getStatusMessage()));
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map(e);
        }
    }

    @Override
    public Credentials validate() throws BackgroundException {
        final Credentials credentials = super.validate();
        final OAuthTokens tokens = credentials.getOauth();
        final String accessToken = tokens.getAccessToken();
        final PreferencesReader preferences = new HostPreferences(bookmark);
        final String tokenExchangeClientId = preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE);
        if(tokenExchangeClientId.isEmpty()) {
            return credentials;
        }
        try {
            final DecodedJWT jwt = JWT.decode(accessToken);

            final String aud = jwt.getClaim(RegisteredClaims.AUDIENCE).asString();
            final String azp = jwt.getClaim(OIDC_AUTHORIZED_PARTY).asString();

            final boolean audNotUnique = aud == null; // either multiple audiences or none
            // do exchange if aud is not unique or azp is not equal to aud
            if(audNotUnique || !aud.equals(azp)) {
                return credentials.withOauth(this.exchange(tokens));
            }
        }
        catch(JWTDecodeException e) {
            throw new LoginFailureException("Invalid JWT or JSON format in authentication token", e);
        }
        return credentials;
    }
}
