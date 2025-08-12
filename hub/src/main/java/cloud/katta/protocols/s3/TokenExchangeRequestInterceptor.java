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

import org.apache.http.client.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageResourceApi;
import cloud.katta.client.model.AccessTokenResponse;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * Exchange OIDC token to scoped token using OAuth 2.0 Token Exchange. Used for S3-STS in Katta.
 */
public class TokenExchangeRequestInterceptor extends OAuth2RequestInterceptor {
    private static final Logger log = LogManager.getLogger(TokenExchangeRequestInterceptor.class);

    /**
     * The party to which the ID Token was issued
     * <a href="https://openid.net/specs/openid-connect-core-1_0.html">...</a>
     */
    public static final String OIDC_AUTHORIZED_PARTY = "azp";

    private final Host bookmark;

    public TokenExchangeRequestInterceptor(final HttpClient client, final Host bookmark, final LoginCallback prompt) throws LoginCanceledException {
        super(client, bookmark, prompt);
        this.bookmark = bookmark;
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
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_VAULT
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_BASEPATH
     */
    public OAuthTokens exchange(final OAuthTokens previous) throws BackgroundException {
        log.info("Exchange tokens {} for {}", previous, bookmark);
        final PreferencesReader preferences = new HostPreferences(bookmark);
        final HubSession hub = bookmark.getProtocol().getFeature(HubSession.class);
        log.debug("Exchange token with hub {}", hub);
        final StorageResourceApi api = new StorageResourceApi(hub.getClient());
        try {
            AccessTokenResponse tokenExchangeResponse = api.apiStorageS3TokenPost(preferences.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_VAULT));
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
