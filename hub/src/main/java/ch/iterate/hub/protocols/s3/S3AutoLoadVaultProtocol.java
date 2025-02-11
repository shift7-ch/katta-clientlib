/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

import ch.cyberduck.core.CredentialsConfigurator;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.s3.S3Protocol;

import com.google.auto.service.AutoService;

@AutoService(Protocol.class)
public class S3AutoLoadVaultProtocol extends S3Protocol {

    // Token exchange
    public static final String OAUTH_TOKENEXCHANGE = "oauth.tokenexchange";
    public static final String OAUTH_TOKENEXCHANGE_AUDIENCE = "oauth.tokenexchange.audience";
    public static final String OAUTH_TOKENEXCHANGE_ADDITIONAL_SCOPES = "oauth.tokenexchange.additional_scopes";

    // STS assume role with web identity from Cyberduck core (AWS + MinIO)
    public static final String S3_ASSUMEROLE_ROLEARN = "s3.assumerole.rolearn";
    public static final String S3_ASSUMEROLE_ROLESESSIONNAME = "s3.assumerole.rolesessionname";
    public static final String S3_ASSUMEROLE_DURATIONSECONDS = "s3.assumerole.durationseconds";
    public static final String S3_ASSUMEROLE_POLICY = "s3.assumerole.policy";
    // STS role chaining (AWS only)
    public static final String S3_ASSUMEROLE_ROLEARN_2 = "s3.assumerole.rolearn.2";

    private final String authorization;

    public S3AutoLoadVaultProtocol() {
        this("AuthorizationCode");
    }

    public S3AutoLoadVaultProtocol(final String authorization) {
        this.authorization = authorization;
    }

    @Override
    public String getIdentifier() {
        return "katta-s3";
    }

    @Override
    public Type getType() {
        return Type.s3;
    }

    @Override
    public String getPrefix() {
        return String.format("%s.%s", S3AutoLoadVaultProtocol.class.getPackage().getName(), "S3AutoLoadVault");
    }

    @Override
    public String getAuthorization() {
        return authorization;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getFeature(final Class<T> type) {
        if(type == CredentialsConfigurator.class) {
            return (T) CredentialsConfigurator.DISABLED;
        }
        return super.getFeature(type);
    }
}
