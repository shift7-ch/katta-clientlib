/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.s3.S3Protocol;

import org.apache.commons.lang3.StringUtils;

import com.google.auto.service.AutoService;

@AutoService(Protocol.class)
public class S3AutoLoadVaultProtocol extends S3Protocol {

    private final String authorization;

    @Override
    public String getName() {
        return "S3AutoLoadVault";
    }

    @Override
    public String getIdentifier() {
        return "s3-hub";
    }

    public Protocol.Type getType() {
        return Type.s3;
    }

    public S3AutoLoadVaultProtocol() {
        this("AuthorizationCode");
    }

    public S3AutoLoadVaultProtocol(final String authorization) {
        this.authorization = authorization;
    }

    @Override
    public String getPrefix() {
        return String.format("%s.%s", this.getClass().getPackage().getName(), StringUtils.capitalize(this.getName()));
    }

    @Override
    public String getAuthorization() {
        return authorization;
    }

    @Override
    public String disk() {
        return String.format("cryptomator.tiff");
    }

}
