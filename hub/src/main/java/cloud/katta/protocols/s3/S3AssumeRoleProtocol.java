/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.CredentialsConfigurator;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.s3.S3Protocol;

import com.google.auto.service.AutoService;

@AutoService(Protocol.class)
public class S3AssumeRoleProtocol extends S3Protocol {

    // Token exchange
    public static final String OAUTH_TOKENEXCHANGE = "oauth.tokenexchange";

    // STS assume role with web identity resource name
    public static final String S3_ASSUMEROLE_ROLEARN_WEBIDENTITY = Profile.STS_ROLE_ARN_PROPERTY_KEY;
    public static final String S3_ASSUMEROLE_DURATIONSECONDS = Profile.STS_DURATION_SECONDS_PROPERTY_KEY;
    // STS assume role chaining (AWS only)
    public static final String S3_ASSUMEROLE_ROLEARN_TAG = "s3.assumerole.rolearn.tag";
    public static final String S3_ASSUMEROLE_ROLEARN_CREATE_BUCKET = "s3.assumerole.rolearn.createbucket";

    @Override
    public String getIdentifier() {
        return "s3-assumerole";
    }

    @Override
    public Type getType() {
        return Type.s3;
    }

    @Override
    public String getPrefix() {
        return String.format("%s.%s", S3AssumeRoleProtocol.class.getPackage().getName(), "S3AssumeRole");
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
