/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSRequestInterceptor;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class S3AssumeRoleSession extends S3Session {
    private static final Logger log = LogManager.getLogger(S3AssumeRoleSession.class);

    public S3AssumeRoleSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    /**
     * Configured by default with credentials strategy using assume role with web identity followed by
     * exchanging the retrieved OIDC token with scoped OAuth tokens to obtain temporary credentials from security
     * token server (STS)
     *
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE
     * @see S3AssumeRoleProtocol#S3_ASSUMEROLE_ROLEARN_TAG
     */
    @Override
    protected S3CredentialsStrategy configureCredentialsStrategy(final HttpClientBuilder configuration, final LoginCallback prompt) throws LoginCanceledException {
        if(host.getProtocol().isOAuthConfigurable()) {
            // Shared OAuth tokens
            final OAuth2RequestInterceptor oauth = host.getProtocol().getFeature(OAuth2RequestInterceptor.class);
            log.debug("Register interceptor {}", oauth);
            configuration.addInterceptorLast(oauth);
            final STSRequestInterceptor sts = new STSChainedAssumeRoleRequestInterceptor(hub, vaultId, host, trust, key, prompt);
            log.debug("Register interceptor {}", sts);
            configuration.addInterceptorLast(sts);
            return sts;
        }
        return super.configureCredentialsStrategy(configuration, prompt);
    }
}
