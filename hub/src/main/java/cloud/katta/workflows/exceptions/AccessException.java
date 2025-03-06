/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows.exceptions;

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
