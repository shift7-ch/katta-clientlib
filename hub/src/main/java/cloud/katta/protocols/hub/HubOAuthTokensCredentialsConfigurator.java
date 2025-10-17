/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.CredentialsConfigurator;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.OAuthTokens;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubOAuthTokensCredentialsConfigurator implements CredentialsConfigurator {
    private static final Logger log = LogManager.getLogger(HubOAuthTokensCredentialsConfigurator.class);

    private final HostPasswordStore keychain;
    private final Host host;

    private OAuthTokens tokens = OAuthTokens.EMPTY;

    public HubOAuthTokensCredentialsConfigurator(final HostPasswordStore keychain, final Host host) {
        this.keychain = keychain;
        this.host = host;
    }

    @Override
    public Credentials configure(final Host host) {
        return new Credentials(host.getCredentials()).setOauth(tokens);
    }

    @Override
    public CredentialsConfigurator reload() {
        if(tokens.isExpired()) {
            log.debug("Reload expired tokens from keychain for {}", host);
            tokens = keychain.findOAuthTokens(host);
            log.debug("Retrieved tokens {}", tokens);
        }
        return this;
    }
}
