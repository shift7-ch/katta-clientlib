/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
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

import java.util.UUID;

import cloud.katta.protocols.hub.HubSession;

public class S3AssumeRoleSession extends S3Session {
    private static final Logger log = LogManager.getLogger(S3AssumeRoleSession.class);

    private final HubSession hub;
    /**
     * Shared OAuth tokens
     */
    private final OAuth2RequestInterceptor oauth;
    private final UUID vaultId;

    public S3AssumeRoleSession(final HubSession hub, final UUID vaultId, final Host host) {
        super(host, hub.getFeature(X509TrustManager.class), hub.getFeature(X509KeyManager.class));
        this.hub = hub;
        this.oauth = hub.getFeature(OAuth2RequestInterceptor.class);
        this.vaultId = vaultId;
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
            log.debug("Register interceptor {}", oauth);
            configuration.addInterceptorLast(oauth);
            final STSRequestInterceptor sts = new STSChainedAssumeRoleRequestInterceptor(hub, oauth, vaultId, host, trust, key, prompt);
            log.debug("Register interceptor {}", sts);
            configuration.addInterceptorLast(sts);
            return sts;
        }
        return super.configureCredentialsStrategy(configuration, prompt);
    }
}
