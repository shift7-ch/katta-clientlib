/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.common;

public class Defaults {
    // role names restricted to 64 characters
    public static final String CREATE_BUCKET_ROLE_NAME_INFIX = "create-bucket";
    public static final String ACCESS_BUCKET_ROLE_NAME_INFIX = "access-bucket";

    public static final String ASSUME_ROLE_WITH_WEB_IDENTITY_ROLE_SUFFIX = "-a-role-web-identity";
    public static final String ASSUME_ROLE_TAGGED_SESSION_ROLE_SUFFIX = "-a-role-tagged-session";

    public static final String REQUEST_TAG = "Vault";
}
