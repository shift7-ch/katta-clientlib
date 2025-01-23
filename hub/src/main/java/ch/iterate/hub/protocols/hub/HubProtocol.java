/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub;

import ch.cyberduck.core.AbstractProtocol;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.Scheme;

import com.google.auto.service.AutoService;

@AutoService(Protocol.class)
public class HubProtocol extends AbstractProtocol {

    @Override
    public String getIdentifier() {
        return "hub";
    }

    @Override
    public String getName() {
        return "Katta";
    }

    @Override
    public String getDescription() {
        return "Katta";
    }

    @Override
    public String getPrefix() {
        return String.format("%s.%s", HubProtocol.class.getPackage().getName(), "Hub");
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
    public boolean isOAuthConfigurable() {
        return true;
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
}
