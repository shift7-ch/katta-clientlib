/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.AsciiRandomStringService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.TemporaryAccessTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.preferences.PreferencesReader;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.sts.STSAssumeRoleCredentialsRequestInterceptor;
import ch.cyberduck.core.sts.STSExceptionMappingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

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

/**
 * Assume role with temporary credentials obtained using OIDC token from security token service (STS)
 */
public class STSChainedAssumeRoleRequestInterceptor extends STSAssumeRoleCredentialsRequestInterceptor {
    private static final Logger log = LogManager.getLogger(STSChainedAssumeRoleRequestInterceptor.class);

    private final Host bookmark;
    private final AWSSecurityTokenService service;

    public STSChainedAssumeRoleRequestInterceptor(final OAuth2RequestInterceptor oauth, final S3Session session,
                                                  final AWSSecurityTokenService service, final LoginCallback prompt) {
        super(oauth, session, service, prompt);
        this.service = service;
        this.bookmark = session.getHost();
    }

    @Override
    public TemporaryAccessTokens authorize(final OAuthTokens oauth) throws BackgroundException {
        return this.authorize(oauth, super.authorize(oauth));
    }

    @Override
    public TemporaryAccessTokens refresh(final OAuthTokens oauth) throws BackgroundException {
        return this.authorize(oauth, super.refresh(oauth));
    }

    /**
     * Assume role with previously obtained temporary access token
     *
     * @param tokens Session credentials
     * @return Temporary scoped access tokens
     * @throws ch.cyberduck.core.exception.ExpiredTokenException Expired identity
     * @throws ch.cyberduck.core.exception.LoginFailureException Authorization failure
     */
    public TemporaryAccessTokens authorize(final OAuthTokens oauth, final TemporaryAccessTokens tokens) throws BackgroundException {
        final AssumeRoleRequest request = new AssumeRoleRequest()
                .withRequestCredentialsProvider(new AWSSessionCredentialsProvider() {
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
        if(log.isDebugEnabled()) {
            log.debug(String.format("Chained assume role for %s", bookmark));
        }
        log.debug(String.format("Assume role with temporary credentials %s", tokens));
        final PreferencesReader preferences = new HostPreferences(bookmark);
        if(preferences.getInteger(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_DURATIONSECONDS) != -1) {
            request.setDurationSeconds(preferences.getInteger(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_DURATIONSECONDS));
        }
        if(StringUtils.isNotBlank(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_POLICY))) {
            request.setPolicy(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_POLICY));
        }
        request.setRoleArn(preferences.getProperty(S3AutoLoadVaultProtocol.S3_ASSUMEROLE_ROLEARN_2));
        final String sub;
        final String identityToken = this.getWebIdentityToken(oauth);
        try {
            sub = JWT.decode(identityToken).getSubject();
        }
        catch(JWTDecodeException e) {
            log.warn(String.format("Failure %s decoding JWT %s", e, identityToken));
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
                log.warn(String.format("Missing subject in decoding JWT %s", identityToken));
                request.setRoleSessionName(new AsciiRandomStringService().random());
            }
        }
        if(StringUtils.isNotBlank(preferences.getProperty("s3.assumerole.tag"))) {
            request.setTags(Collections.singletonList(new Tag()
                    .withKey(preferences.getProperty("s3.assumerole.tag"))
                    .withValue(preferences.getProperty(S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES))));
        }
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Use request %s", request));
            }
            final AssumeRoleResult result = service.assumeRole(request);
            if(log.isDebugEnabled()) {
                log.debug(String.format("Received assume role result %s for host %s", result, bookmark));
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
}
