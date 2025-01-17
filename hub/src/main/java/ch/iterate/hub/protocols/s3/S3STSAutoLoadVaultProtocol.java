/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.Protocol;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;

@AutoService(Protocol.class)
public class S3STSAutoLoadVaultProtocol extends S3AutoLoadVaultProtocol {

    public S3STSAutoLoadVaultProtocol() {
        this("AuthorizationCode");
    }

    public S3STSAutoLoadVaultProtocol(final String authorization) {
        super(authorization);
    }

    @Override
    public String getIdentifier() {
        return "s3-hub-sts";
    }

    @Override
    public String getName() {
        return "S3STSAutoLoadVault";
    }

    public String getDescription() {
        return LocaleFactory.localizedString("S3-STS", "S3");
    }

    @Override
    public Protocol.Type getType() {
        return Type.s3;
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
    public String getPrefix() {
        return String.format("%s.%s", this.getClass().getPackage().getName(), StringUtils.capitalize(this.getName()));
    }


    @Override
    public String getOAuthClientSecret() {
        // N.B. required in OAuth2RequestInterceptor/STSAssumeRoleCredentialsRequestInterceptor in the S3Session
        // https://github.com/iterate-ch/docs/issues/403 null will ask for client secret!
        return "";
    }

    @Override
    public List<String> getOAuthScopes() {
        // get ID token as well
        // N.B. required in OAuth2RequestInterceptor/STSAssumeRoleCredentialsRequestInterceptor in the S3Session
        return Collections.singletonList("openid");
    }


    @Override
    public String disk() {
        return String.format("%s.tiff", Type.s3.name());
    }
}
