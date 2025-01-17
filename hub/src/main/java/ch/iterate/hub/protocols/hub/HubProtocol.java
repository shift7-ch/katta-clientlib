/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractProtocol;
import ch.cyberduck.core.CredentialsConfigurator;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.Scheme;

import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;


@AutoService(Protocol.class)
public class HubProtocol extends AbstractProtocol {
    private final String authorization;
    private final CredentialsConfigurator c;

    public HubProtocol() {
        this("AuthorizationCode");
    }

    public HubProtocol(final String authorization) {
        this.authorization = authorization;
        this.c = null;
    }

    public HubProtocol(final String authorization, final CredentialsConfigurator c) {
        this.authorization = authorization;
        this.c = c;
    }

    @Override
    public String getIdentifier() {
        return "hub";
    }


    @Override
    public String getName() {
        return "Cipherduck Hub";
    }

    @Override
    public String getPrefix() {
        return String.format("%s.%s", HubProtocol.class.getPackage().getName(), "Hub");
    }


    @Override
    public String getDescription() {
        return "Cipherduck Hub";
    }

    @Override
    public Scheme getScheme() {
        return Scheme.https;
    }

    @Override
    public Type getType() {
        return Type.none;
    }

    @Override
    public String getDefaultHostname() {
        return "localhost";
    }

    @Override
    public String getAuthorization() {
        return authorization;
    }

    @Override
    public boolean isHostnameConfigurable() {
        return true;
    }

    @Override
    public boolean isUsernameConfigurable() {
        return false;
    }

    @Override
    public boolean isPasswordConfigurable() {
        return false;
    }

    @Override
    public boolean isTokenConfigurable() {
        return false;
    }

    @Override
    public String getOAuthRedirectUrl() {
        return "x-katta-action:oauth";
    }


    @Override
    public String getOAuthClientSecret() {
        // https://github.com/iterate-ch/docs/issues/403 null will ask for client secret!
        return "";
    }

    @Override
    public List<String> getOAuthScopes() {
        // get ID token as well
        return Collections.singletonList("openid");
    }

    @Override
    public String disk() {
        return "cryptomator.tiff";
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getFeature(final Class<T> type) {
        if(type == CredentialsConfigurator.class && null != c) {
            return (T) c;
        }
        return super.getFeature(type);
    }

}
