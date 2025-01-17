/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.s3;

public class CipherduckHostCustomProperties {

    // vault auto-loading
    public static final String HUB_URL = "hubURL";
    public static final String HUB_USERNAME = "hubUsername";

    // hub host collection
    public static final String HUB_UUID = "hubUUID";

    // token exchange
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

}
