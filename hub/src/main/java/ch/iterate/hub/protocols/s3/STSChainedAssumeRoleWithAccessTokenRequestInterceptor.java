/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.AsciiRandomStringService;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.TemporaryAccessTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.sts.STSAssumeRoleCredentialsRequestInterceptor;
import ch.cyberduck.core.sts.STSExceptionMappingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSSessionCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Tag;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

/**
 * AssumeRoleWithWebIdentity optionally chained by AssumeRole.
 * <p>
 * Use OAuth 2.0 access token may also be used instead of default OIDC ID token for AssumeRoleWithWebIdentity.
 * In Cipherduck, we get an access token from token exchange.
 * See https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html
 */
public class STSChainedAssumeRoleWithAccessTokenRequestInterceptor extends STSAssumeRoleCredentialsRequestInterceptor implements S3CredentialsStrategy, HttpRequestInterceptor {

    // https://datatracker.ietf.org/doc/html/rfc8693#name-request
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID = "client_id";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_AUDIENCE = "audience";
    public static final String OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN = "subject_token";


    private static final Logger log = LogManager.getLogger(STSChainedAssumeRoleWithAccessTokenRequestInterceptor.class);
    private final S3Session session;
    private final LoginCallback prompt;
    private final AWSSecurityTokenService service;

    public STSChainedAssumeRoleWithAccessTokenRequestInterceptor(final OAuth2RequestInterceptor oauth, final S3Session session,
                                                                 final AWSSecurityTokenService service, final LoginCallback prompt) {
        super(oauth, session, service, prompt);
        this.session = session;
        this.prompt = prompt;
        this.service = service;
    }

    private OAuthTokens tokenExchange(final OAuthTokens previous) {

        final Host bookmark = session.getHost();

        log.info(String.format("Token exchange for %s", bookmark));

        HostPreferences preferences = new HostPreferences(bookmark);

        if(!preferences.getBoolean(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE)) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Skipping token exchange for %s", bookmark));
            }
            return previous;
        }

        final HttpTransport transport = new ApacheHttpTransport(HttpClientBuilder.create().build());
        final JsonFactory json = new GsonFactory();


        final String tokenServerUrl = bookmark.getProtocol().getOAuthTokenUrl();

        final String clientid = bookmark.getProtocol().getOAuthClientId();

        final TokenRequest tokenExchangeRequest = new TokenRequest(
                transport,
                json,
                new GenericUrl(tokenServerUrl),
                OAUTH_GRANT_TYPE_TOKEN_EXCHANGE
        );
        tokenExchangeRequest.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_CLIENT_ID, clientid);
        if(!StringUtils.isEmpty(bookmark.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE))) {
            tokenExchangeRequest.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_AUDIENCE, bookmark.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_AUDIENCE));
        }
        // N.B. token exchange with Id token does not work!
        tokenExchangeRequest.set(OAUTH_GRANT_TYPE_TOKEN_EXCHANGE_SUBJECT_TOKEN, previous.getAccessToken());

        final ArrayList<String> scopes = new ArrayList<>(bookmark.getProtocol().getOAuthScopes());
        if(!StringUtils.isEmpty(bookmark.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES))) {
            scopes.addAll(Arrays.asList(bookmark.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES).split(" ")));
        }
        tokenExchangeRequest.setScopes(scopes);

        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Token exchange request %s for %s", tokenExchangeRequest, bookmark));
            }
            if(log.isDebugEnabled()) {
                String s = "curl -X POST";
                for(Map.Entry<String, Object> entry : tokenExchangeRequest.entrySet()) {
                    s += String.format(" -d \"%s=%s\"", entry.getKey(), entry.getValue());
                }
                s += " " + tokenServerUrl;
                log.debug(s);
            }
            final TokenResponse tokenExchangeResponse = tokenExchangeRequest.execute();
            final OAuthTokens tokens = new OAuthTokens(tokenExchangeResponse.getAccessToken(), tokenExchangeResponse.getRefreshToken(), System.currentTimeMillis() + tokenExchangeResponse.getExpiresInSeconds() * 1000);
            if(log.isDebugEnabled()) {
                log.debug(String.format("Received exchanged token %s for %s", previous, bookmark));
            }
            return tokens;
        }
        catch(IOException e) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Not received exchanged token %s for %s", previous, bookmark));
            }
            throw new RuntimeException(e);
        }

    }

    public TemporaryAccessTokens authorize(final OAuthTokens previous) throws BackgroundException {
        final Host bookmark = session.getHost();

        final OAuthTokens oauth = this.tokenExchange(previous);

        log.info(String.format("Get temporary access tokens from STS for %s", bookmark));

        final TemporaryAccessTokens tokens = super.authorize(oauth);

        if(StringUtils.isBlank(bookmark.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN_2))) {
            // no role-chaining (MinIO)
            return tokens;
        }


        final AssumeRoleRequest request = new AssumeRoleRequest();
        if(log.isDebugEnabled()) {
            log.debug(String.format("Chained assume role for %s", bookmark));
        }
        request.setRequestCredentialsProvider(new AWSSessionCredentialsProvider() {
            @Override
            public AWSSessionCredentials getCredentials() {
                return new BasicSessionCredentials(
                        tokens.getAccessKeyId(),
                        tokens.getSecretAccessKey(),
                        tokens.getSessionToken());
            }

            @Override
            public void refresh() {
                // nothing to do
            }
        });
        log.debug(String.format("Assume role with temporary credentials %s", tokens));
        final HostPreferences preferences = new HostPreferences(bookmark);
        if(preferences.getInteger(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_DURATIONSECONDS) != -1) {
            request.setDurationSeconds(preferences.getInteger(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_DURATIONSECONDS));
        }
        if(StringUtils.isNotBlank(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_POLICY))) {
            request.setPolicy(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_POLICY));
        }
        if(StringUtils.isNotBlank(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN_2))) {
            request.setRoleArn(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN_2));
        }
        else {
            if(StringUtils.EMPTY.equals(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN_2))) {
                // When defined in connection profile but with empty value
                if(log.isDebugEnabled()) {
                    log.debug("Prompt for Role ARN");
                }
                final Credentials input = prompt.prompt(bookmark,
                        LocaleFactory.localizedString("Role Amazon Resource Name (ARN)", "Credentials"),
                        LocaleFactory.localizedString("Provide additional login credentials", "Credentials"),
                        new LoginOptions().icon(bookmark.getProtocol().disk()));
                if(input.isSaved()) {
                    bookmark.setProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN_2, input.getPassword());
                }
                request.setRoleArn(input.getPassword());
            }
        }
        final String sub;
        try {
            sub = JWT.decode(oauth.getAccessToken()).getSubject();
        }
        catch(JWTDecodeException e) {
            log.warn(String.format("Failure %s decoding JWT %s", e, oauth.getIdToken()));
            throw new LoginFailureException("Invalid JWT or JSON format in authentication token", e);
        }
        if(StringUtils.isNotBlank(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLESESSIONNAME))) {
            request.setRoleSessionName(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLESESSIONNAME));
        }
        else {
            if(StringUtils.isNotBlank(sub)) {
                request.setRoleSessionName(sub);
            }
            else {
                log.warn(String.format("Missing subject in decoding JWT %s", oauth.getIdToken()));
                request.setRoleSessionName(new AsciiRandomStringService().random());
            }
        }
        if(StringUtils.isNotBlank(preferences.getProperty("s3.assumerole.tag"))) {
            request.setTags(Collections.singletonList(new Tag().withKey(preferences.getProperty("s3.assumerole.tag")).withValue(preferences.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES))));
        }

        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Use request %s", request));
            }
            final AssumeRoleResult result = service.assumeRole(request);
            if(log.isDebugEnabled()) {
                log.debug(String.format("Received assume role result %s for host %s", result.getCredentials(), bookmark));
            }
            return new TemporaryAccessTokens(result.getCredentials().getAccessKeyId(),
                    result.getCredentials().getSecretAccessKey(),
                    result.getCredentials().getSessionToken(),
                    result.getCredentials().getExpiration().getTime());
        }
        catch(AWSSecurityTokenServiceException e) {
            throw new STSExceptionMappingService().map(e);
        }

    }

    @Override
    protected String getWebIdentityToken(final OAuthTokens oauth) {
        return oauth.getAccessToken();
    }
}
