/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.TemporaryAccessTokens;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.preferences.PreferencesReader;
import ch.cyberduck.core.preferences.ProxyPreferencesReader;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSAssumeRoleWithWebIdentityRequestInterceptor;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Assume role with temporary credentials obtained using OIDC token from security token service (STS)
 */
public class STSChainedAssumeRoleRequestInterceptor extends STSAssumeRoleWithWebIdentityRequestInterceptor {
    private static final Logger log = LogManager.getLogger(STSChainedAssumeRoleRequestInterceptor.class);

    private final Host bookmark;

    public STSChainedAssumeRoleRequestInterceptor(final OAuth2RequestInterceptor oauth, final Host host,
                                                  final X509TrustManager trust, final X509KeyManager key,
                                                  final LoginCallback prompt) {
        super(oauth, host, trust, key, prompt);
        this.bookmark = host;
    }

    /**
     * Assume role with previously obtained temporary access token
     *
     * @param credentials Session credentials
     * @return Temporary scoped access tokens
     * @throws ch.cyberduck.core.exception.ExpiredTokenException Expired identity
     * @throws ch.cyberduck.core.exception.LoginFailureException Authorization failure
     * @see S3AssumeRoleProtocol#S3_ASSUMEROLE_ROLEARN_TAG
     * @see S3AssumeRoleProtocol#S3_ASSUMEROLE_ROLEARN_CREATE_BUCKET
     */
    @Override
    public TemporaryAccessTokens assumeRoleWithWebIdentity(final Credentials credentials) throws BackgroundException {
        final PreferencesReader settings = new ProxyPreferencesReader(bookmark, credentials);
        final TemporaryAccessTokens tokens = super.assumeRoleWithWebIdentity(credentials
                .withProperty(Profile.STS_ROLE_ARN_PROPERTY_KEY, settings.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_WEBIDENTITY)));
        if(StringUtils.isNotBlank(settings.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_TAG))) {
            log.debug("Assume role with temporary credentials {}", tokens);
            // Assume role with previously obtained temporary access token
            return super.assumeRole(credentials.withTokens(tokens)
                    .withProperty(Profile.STS_ROLE_ARN_PROPERTY_KEY, settings.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_TAG))
                    .withProperty(Profile.STS_TAGS_PROPERTY_KEY, String.format("%s=%s", HostPreferencesFactory.get(bookmark).getProperty("s3.assumerole.rolearn.tag.vaultid.key"),
                            settings.getProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_VAULT)))
            );
        }
        return tokens;
    }
}
