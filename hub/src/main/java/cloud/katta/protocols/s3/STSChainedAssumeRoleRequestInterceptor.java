/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.TemporaryAccessTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.preferences.PreferencesReader;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSAssumeRoleWithWebIdentityRequestInterceptor;

import org.apache.commons.lang3.StringUtils;
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
 * Assume role with temporary credentials obtained using OIDC token from security token service (STS)
 */
public class STSChainedAssumeRoleRequestInterceptor extends STSAssumeRoleWithWebIdentityRequestInterceptor {
    private static final Logger log = LogManager.getLogger(STSChainedAssumeRoleRequestInterceptor.class);

    /**
     * The party to which the ID Token was issued
     * <a href="https://openid.net/specs/openid-connect-core-1_0.html">...</a>
     */
    private static final String OIDC_AUTHORIZED_PARTY = "azp";

    private final Host bookmark;
    private final String vaultId;

    public STSChainedAssumeRoleRequestInterceptor(final OAuth2RequestInterceptor oauth, final Host host,
                                                  final X509TrustManager trust, final X509KeyManager key,
                                                  final LoginCallback prompt) {
        super(oauth, host, trust, key, prompt);
        this.bookmark = host;
        this.vaultId = HostPreferencesFactory.get(host).getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_VAULT);
    }

    /**
     * Assume role with previously obtained temporary access token
     *
     * @param oauth OIDC tokens
     * @return Temporary scoped access tokens
     * @throws ch.cyberduck.core.exception.ExpiredTokenException Expired identity
     * @throws ch.cyberduck.core.exception.LoginFailureException Authorization failure
     * @see S3AssumeRoleProtocol#S3_ASSUMEROLE_ROLEARN_TAG
     * @see S3AssumeRoleProtocol#S3_ASSUMEROLE_ROLEARN_CREATE_BUCKET
     */
    @Override
    public TemporaryAccessTokens assumeRoleWithWebIdentity(final OAuthTokens oauth, final String roleArn) throws BackgroundException {
        final PreferencesReader settings = HostPreferencesFactory.get(bookmark);
        final TemporaryAccessTokens tokens = super.assumeRoleWithWebIdentity(this.tokenExchange(oauth), settings.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_WEBIDENTITY));
        if(StringUtils.isNotBlank(settings.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_TAG))) {
            log.debug("Assume role with temporary credentials {}", tokens);
            // Assume role with previously obtained temporary access token
            return super.assumeRole(credentials.setTokens(tokens)
                            .setProperty(Profile.STS_TAGS_PROPERTY_KEY, String.format("%s=%s", HostPreferencesFactory.get(bookmark).getProperty("s3.assumerole.rolearn.tag.vaultid.key"), vaultId)),
                    settings.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_TAG));
        }
        log.warn("No vault tag set. Skip assuming role with temporary credentials {} for {}", tokens, bookmark);
        return tokens;
    }

    /**
     * Perform OAuth 2.0 Token Exchange
     *
     * @return New tokens
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE_VAULT
     */
    private OAuthTokens tokenExchange(final OAuthTokens tokens) throws BackgroundException {
        final PreferencesReader settings = HostPreferencesFactory.get(bookmark);
        if(settings.getBoolean(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE)) {
            if(this.isTokenExchangeRequired(tokens)) {
                log.info("Exchange tokens for {}", bookmark);
                final HubSession hub = bookmark.getProtocol().getFeature(HubSession.class);
                log.debug("Exchange token with hub {}", hub);
                final StorageResourceApi api = new StorageResourceApi(hub.getClient());
                try {
                    final AccessTokenResponse tokenExchangeResponse = api.apiStorageS3TokenPost(vaultId);
                    // N.B. token exchange with Id token does not work!
                    final OAuthTokens exchanged = new OAuthTokens(tokenExchangeResponse.getAccessToken(),
                            tokenExchangeResponse.getRefreshToken(),
                            tokenExchangeResponse.getExpiresIn() != null ? System.currentTimeMillis() + tokenExchangeResponse.getExpiresIn() * 1000 : null);
                    log.debug("Received exchanged token {} for {}", exchanged, bookmark);
                    return exchanged;
                }
                catch(ApiException e) {
                    throw new HubExceptionMappingService().map(e);
                }
            }
        }
        return tokens;
    }

    private boolean isTokenExchangeRequired(final OAuthTokens tokens) throws BackgroundException {
        final String accessToken = tokens.getAccessToken();
        try {
            final DecodedJWT jwt = JWT.decode(accessToken);
            final List<String> auds = jwt.getAudience();
            final String azp = jwt.getClaim(OIDC_AUTHORIZED_PARTY).asString();
            log.debug("Decoded JWT {} with audience {} and azp {}", jwt, Arrays.toString(auds.toArray()), azp);
            final boolean audNotUnique = 1 != auds.size(); // either multiple audiences or none
            // do exchange if aud is not unique or azp is not equal to aud
            if(audNotUnique || !auds.get(0).equals(azp)) {
                log.debug("None or multiple audiences found {} or audience differs from azp {}", Arrays.toString(auds.toArray()), azp);
                return true;
            }
        }
        catch(JWTDecodeException e) {
            throw new LoginFailureException("Invalid JWT or JSON format in authentication token", e);
        }
        return false;
    }
}
