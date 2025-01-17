/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows.exceptions;

public class AccessException extends Exception {
    public AccessException(String message) {
        super(message);
    }

    public AccessException(final Throwable cause) {
        super(cause);
    }

    public AccessException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
