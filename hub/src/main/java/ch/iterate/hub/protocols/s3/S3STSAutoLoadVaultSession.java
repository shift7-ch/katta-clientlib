/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.aws.CustomClientConfiguration;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.s3.S3AuthenticationResponseInterceptor;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.ssl.ThreadLocalHostnameDelegatingTrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSAssumeRoleCredentialsRequestInterceptor;

import org.apache.http.impl.client.HttpClientBuilder;

import ch.iterate.hub.oauth.STSChainedAssumeRoleWithAccessTokenRequestInterceptor;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;


public class S3STSAutoLoadVaultSession extends S3AutoLoadVaultSession {
    public S3STSAutoLoadVaultSession(final Host host) {
        super(host);
    }

    public S3STSAutoLoadVaultSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }


    @Override
    protected S3CredentialsStrategy configureCredentialsStrategy(final ProxyFinder proxy, final HttpClientBuilder configuration,
                                                                 final LoginCallback prompt) throws LoginCanceledException {
        final OAuth2RequestInterceptor oauth = new OAuth2RequestInterceptor(builder.build(ProxyFactory.get(), this, prompt).build(), host, prompt)
                .withRedirectUri(host.getProtocol().getOAuthRedirectUrl());
        if(host.getProtocol().getAuthorization() != null) {
            oauth.withFlowType(OAuth2AuthorizationService.FlowType.valueOf(host.getProtocol().getAuthorization()));
        }
        configuration.addInterceptorLast(oauth);

        final STSAssumeRoleCredentialsRequestInterceptor interceptor
                = new STSChainedAssumeRoleWithAccessTokenRequestInterceptor(oauth, this, AWSSecurityTokenServiceClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(host.getProtocol().getSTSEndpoint(), null))
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .withClientConfiguration(new CustomClientConfiguration(host,
                        new ThreadLocalHostnameDelegatingTrustManager(trust, host.getProtocol().getSTSEndpoint()), key))
                .build(), prompt);
        configuration.addInterceptorLast(interceptor);
        configuration.setServiceUnavailableRetryStrategy(new S3AuthenticationResponseInterceptor(this, interceptor));
        return interceptor;
    }
}
