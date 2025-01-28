/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.http.DefaultHttpResponseExceptionMappingService;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.oauth.OAuthExceptionMappingService;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.preferences.PreferencesReader;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class TokenExchangeRequestInterceptor extends OAuth2RequestInterceptor {
    private static final Logger log = LogManager.getLogger(TokenExchangeRequestInterceptor.class);

    // https://datatracker.ietf.org/doc/html/rfc8693#name-request
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID = "client_id";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_AUDIENCE = "audience";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN = "subject_token";

    private final Host bookmark;
    private final HttpClient client;

    public TokenExchangeRequestInterceptor(final HttpClient client, final Host bookmark, LoginCallback prompt) throws LoginCanceledException {
        super(client, bookmark, prompt);
        this.bookmark = bookmark;
        this.client = client;
    }

    @Override
    public OAuthTokens authorize() throws BackgroundException {
        return this.exchange(super.authorize());
    }

    @Override
    public OAuthTokens refresh(OAuthTokens previous) throws BackgroundException {
        return this.exchange(super.refresh(previous));
    }

    /**
     * Perform OAuth 2.0 Token Exchange
     *
     * @param previous Input tokens retrieved to exchange at the token endpoint
     * @return New tokens
     */
    public OAuthTokens exchange(final OAuthTokens previous) throws BackgroundException {
        log.info(String.format("Exchange tokens %s for %s", previous, bookmark));
        final TokenRequest request = new TokenRequest(
                new ApacheHttpTransport(client),
                new GsonFactory(),
                new GenericUrl(bookmark.getProtocol().getOAuthTokenUrl()),
                OAUTH_GRANT_TYPE_TOKEN_EXCHANGE
        );
        request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID, bookmark.getProtocol().getOAuthClientId());
        final PreferencesReader preferences = new HostPreferences(bookmark);
        if (!StringUtils.isEmpty(preferences.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE))) {
            request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_AUDIENCE, preferences.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE));
        }
        request.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN, previous.getAccessToken());
        final ArrayList<String> scopes = new ArrayList<>(bookmark.getProtocol().getOAuthScopes());
        if (!StringUtils.isEmpty(preferences.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES))) {
            scopes.addAll(Arrays.asList(preferences.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES).split(" ")));
        }
        request.setScopes(scopes);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Token exchange request %s for %s", request, bookmark));
        }
        try {
            final TokenResponse tokenExchangeResponse = request.execute();
            // N.B. token exchange with Id token does not work!
            final OAuthTokens tokens = new OAuthTokens(tokenExchangeResponse.getAccessToken(),
                    tokenExchangeResponse.getRefreshToken(),
                    System.currentTimeMillis() + tokenExchangeResponse.getExpiresInSeconds() * 1000);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Received exchanged token %s for %s", tokens, bookmark));
            }
            return tokens;
        } catch (TokenResponseException e) {
            throw new OAuthExceptionMappingService().map(e);
        } catch (HttpResponseException e) {
            throw new DefaultHttpResponseExceptionMappingService().map(new org.apache.http.client
                    .HttpResponseException(e.getStatusCode(), e.getStatusMessage()));
        } catch (IOException e) {
            throw new DefaultIOExceptionMappingService().map(e);
        }
    }
}
