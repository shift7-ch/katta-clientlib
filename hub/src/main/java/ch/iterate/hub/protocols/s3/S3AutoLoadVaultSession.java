/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.AbstractHostCollection;
import ch.cyberduck.core.BookmarkCollection;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostKeyCallback;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.aws.CustomClientConfiguration;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.exception.LoginFailureException;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.oauth.OAuth2RequestInterceptor;
import ch.cyberduck.core.preferences.HostPreferences;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.proxy.ProxyFinder;
import ch.cyberduck.core.s3.RequestEntityRestStorageService;
import ch.cyberduck.core.s3.S3AuthenticationResponseInterceptor;
import ch.cyberduck.core.s3.S3CredentialsStrategy;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.shared.DefaultPathHomeFeature;
import ch.cyberduck.core.shared.DelegatingHomeFeature;
import ch.cyberduck.core.ssl.ThreadLocalHostnameDelegatingTrustManager;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.sts.STSAssumeRoleCredentialsRequestInterceptor;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultFactory;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.primitives.Bytes;
import com.nimbusds.jose.util.Base64URL;

import static ch.iterate.hub.protocols.s3.S3AutoLoadVaultProtocol.OAUTH_TOKENEXCHANGE;

public class S3AutoLoadVaultSession extends S3Session {
    private static final Logger log = LogManager.getLogger(S3AutoLoadVaultSession.class);

    private final AbstractHostCollection bookmarks = BookmarkCollection.defaultCollection();
    private final HostPasswordStore keychain = PasswordStoreFactory.get();

    private HubSession backend;

    public S3AutoLoadVaultSession(final Host host, final X509TrustManager trust, final X509KeyManager key) {
        super(host, trust, key);
    }

    @Override
    public RequestEntityRestStorageService open(final ProxyFinder proxy, final HostKeyCallback hostcallback, final LoginCallback login, final CancelCallback cancel) throws BackgroundException {
        final Host hub = bookmarks.lookup(host.getProperty(HubSession.HUB_UUID));
        if(null == hub) {
            throw new ConnectionCanceledException(String.format("Missing configuration %s", host.getProperty(HubSession.HUB_UUID)));
        }
        backend = new HubSession(hub, trust, key);
        backend.open(proxy, hostcallback, login, cancel);
        return super.open(proxy, hostcallback, login, cancel);
    }

    @Override
    public void login(final LoginCallback prompt, final CancelCallback cancel) throws BackgroundException {
        try {
            final Credentials credentials = backend.getHost().getCredentials().withOauth(keychain.findOAuthTokens(backend.getHost()));
            log.debug("Login to {} with credentials {}", backend.getHost(), credentials);
            backend.login(prompt, cancel);
            final Path home = new DelegatingHomeFeature(new DefaultPathHomeFeature(host)).find();
            log.debug("Attempting to locate vault in {}", home);
            final Vault vault = VaultFactory.get(home);
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! MUST NEVER BE RELEASED LIKE THIS
            // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 use rawFileKey,rawNameKey as vault key for now (going into cryptolib's Masterkey)
            final UvfMetadataPayload vaultMetadata = new VaultServiceImpl(backend).getVaultMetadataJWE(
                    UUID.fromString(host.getUuid()), new UserKeysServiceImpl(backend).getUserKeys(backend.getHost(), FirstLoginDeviceSetupCallbackFactory.get()));
            byte[] rawFileKey = Base64URL.from(vaultMetadata.seeds().get(vaultMetadata.latestSeed())).decode();
            byte[] rawNameKey = Base64URL.from(vaultMetadata.seeds().get(vaultMetadata.latestSeed())).decode();
            final byte[] vaultKey = Bytes.concat(rawFileKey, rawNameKey);
            registry.add(vault.load(this, new DisabledPasswordCallback() {
                @Override
                public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) {
                    return new VaultCredentials(Base64.getEncoder().encodeToString(vaultKey));
                }
            }));
            backend.close();
        }
        catch(ApiException | SecurityFailure | AccessException e) {
            throw new LoginFailureException(LocaleFactory.localizedString("Login failed", "Credentials"), e);
        }
        super.login(prompt, cancel);
    }

    @Override
    protected S3CredentialsStrategy configureCredentialsStrategy(final ProxyFinder proxy, final HttpClientBuilder configuration,
                                                                 final LoginCallback prompt) throws LoginCanceledException {
        if(host.getProtocol().isOAuthConfigurable()) {
            if(new HostPreferences(host).getBoolean(OAUTH_TOKENEXCHANGE)) {
                final OAuth2RequestInterceptor oauth = new OAuth2RequestInterceptor(builder.build(ProxyFactory.get(), this, prompt).build(), host, prompt)
                        .withRedirectUri(host.getProtocol().getOAuthRedirectUrl());
                if(host.getProtocol().getAuthorization() != null) {
                    oauth.withFlowType(OAuth2AuthorizationService.FlowType.valueOf(host.getProtocol().getAuthorization()));
                }
                configuration.addInterceptorLast(oauth);
                final STSAssumeRoleCredentialsRequestInterceptor interceptor
                        = new STSChainedAssumeRoleWithAccessTokenRequestInterceptor(oauth, this, AWSSecurityTokenServiceClientBuilder.standard()
                        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(host.getProtocol().getSTSEndpoint(), null))
                        .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                        .withClientConfiguration(new CustomClientConfiguration(host,
                                new ThreadLocalHostnameDelegatingTrustManager(trust, host.getProtocol().getSTSEndpoint()), key)).build(), prompt);
                configuration.addInterceptorLast(interceptor);
                configuration.setServiceUnavailableRetryStrategy(new S3AuthenticationResponseInterceptor(this, interceptor));
                return interceptor;
            }
            log.warn("Token exchange disabled for {}", host);
        }
        return super.configureCredentialsStrategy(proxy, configuration, prompt);
    }
}
