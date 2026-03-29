/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSCredentialsStrategy;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class S3AssumeRoleSession extends S3Session {
    private static final Logger log = LogManager.getLogger(S3AssumeRoleSession.class);

    private final STSCredentialsStrategy sts;
    private final OAuth2RequestInterceptor oauth;

    public S3AssumeRoleSession(final Host host, final OAuth2RequestInterceptor oauth, final STSCredentialsStrategy sts,
                               final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
        this.oauth = oauth;
        this.sts = sts;
    }

    /**
     * Configured by default with credentials strategy using assume role with web identity followed by
     * exchanging the retrieved OIDC token with scoped OAuth tokens to obtain temporary credentials from security
     * token server (STS)
     */
    @Override
    protected S3CredentialsStrategy configureCredentialsStrategy(final HttpClientBuilder configuration, final LoginCallback prompt) {
        log.debug("Register interceptor {}", oauth);
        configuration.addInterceptorLast(oauth);
        return sts;
    }
}
