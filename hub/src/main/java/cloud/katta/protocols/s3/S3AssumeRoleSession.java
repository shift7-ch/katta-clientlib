/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.aws.CustomClientConfiguration;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.http.CustomServiceUnavailableRetryStrategy;
import ch.cyberduck.core.http.ExecutionCountServiceUnavailableRetryStrategy;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.s3.S3AuthenticationResponseInterceptor;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.ssl.ThreadLocalHostnameDelegatingTrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSAssumeRoleCredentialsRequestInterceptor;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;

import static cloud.katta.protocols.s3.S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE;

public class S3AssumeRoleSession extends S3Session {
    private static final Logger log = LogManager.getLogger(S3AssumeRoleSession.class);

    public S3AssumeRoleSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    /**
     * Configured by default with credentials strategy using assume role with web identity followed by
     * exchaing the retrieved OIDC token with scoped OAuth tokens to obtain temporary credentials from security
     * token server (STS)
     *
     * @see S3AssumeRoleProtocol#OAUTH_TOKENEXCHANGE
     * @see S3AssumeRoleProtocol#S3_ASSUMEROLE_ROLEARN_2
     */
    @Override
    protected S3CredentialsStrategy configureCredentialsStrategy(final ProxyFinder proxy, final HttpClientBuilder configuration,
                                                                 final LoginCallback prompt) throws LoginCanceledException {
        if(host.getProtocol().isOAuthConfigurable()) {
            final OAuth2RequestInterceptor oauth;
            if(HostPreferencesFactory.get(host).getBoolean(OAUTH_TOKENEXCHANGE)) {
                oauth = new TokenExchangeRequestInterceptor(configuration.build(), host, prompt);
            }
            else {
                oauth = new OAuth2RequestInterceptor(configuration.build(), host, prompt);
            }
            oauth.withRedirectUri(host.getProtocol().getOAuthRedirectUrl());
            if(host.getProtocol().getAuthorization() != null) {
                oauth.withFlowType(OAuth2AuthorizationService.FlowType.valueOf(host.getProtocol().getAuthorization()));
            }
            log.debug("Register interceptor {}", oauth);
            configuration.addInterceptorLast(oauth);
            final AWSSecurityTokenService tokenService = AWSSecurityTokenServiceClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(host.getProtocol().getSTSEndpoint(), null))
                    .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                    .withClientConfiguration(new CustomClientConfiguration(host,
                            new ThreadLocalHostnameDelegatingTrustManager(trust, host.getProtocol().getSTSEndpoint()), key))
                    .build();
            final STSAssumeRoleCredentialsRequestInterceptor sts;
            if(StringUtils.isNotBlank(host.getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_2))) {
                sts = new STSChainedAssumeRoleRequestInterceptor(oauth, this, tokenService, prompt) {
                    @Override
                    protected String getWebIdentityToken(final OAuthTokens oauth) {
                        return oauth.getAccessToken();
                    }
                };
            }
            else {
                sts = new STSAssumeRoleCredentialsRequestInterceptor(oauth, this, tokenService, prompt) {
                    @Override
                    protected String getWebIdentityToken(final OAuthTokens oauth) {
                        return oauth.getAccessToken();
                    }
                };
            }
            log.debug("Register interceptor {}", sts);
            configuration.addInterceptorLast(sts);
            final S3AuthenticationResponseInterceptor interceptor = new S3AuthenticationResponseInterceptor(this, sts);
            configuration.setServiceUnavailableRetryStrategy(new CustomServiceUnavailableRetryStrategy(host,
                    new ExecutionCountServiceUnavailableRetryStrategy(interceptor)));
            return sts;
        }
        return super.configureCredentialsStrategy(proxy, configuration, prompt);
    }
}
