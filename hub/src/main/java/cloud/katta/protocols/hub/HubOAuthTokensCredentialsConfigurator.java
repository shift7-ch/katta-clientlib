/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.CredentialsConfigurator;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.OAuthTokens;

public class HubOAuthTokensCredentialsConfigurator implements CredentialsConfigurator {

    private final HostPasswordStore keychain;
    private final Host host;

    private OAuthTokens tokens;

    public HubOAuthTokensCredentialsConfigurator(final HostPasswordStore keychain, final Host host) {
        this.keychain = keychain;
        this.host = host;
        final Credentials credentials = host.getCredentials();
        // Copy prior reset of credentials after login
        this.tokens = new OAuthTokens(credentials.getOauth().getAccessToken(),
                credentials.getOauth().getRefreshToken(),
                credentials.getOauth().getExpiryInMilliseconds(),
                credentials.getOauth().getIdToken());
    }

        @Override
    public Credentials configure(final Host host) {
        return new Credentials(host.getCredentials()).withOauth(tokens);
    }

    @Override
    public CredentialsConfigurator reload() {
        if(tokens.isExpired()) {
            tokens = keychain.findOAuthTokens(host);
        }
        return this;
    }
}
