/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows.exceptions;

public class SecurityFailure extends Exception {
    public SecurityFailure(String message) {
        super(message);
    }

    public SecurityFailure(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SecurityFailure(final Throwable cause) {
        super(cause);
    }
}
