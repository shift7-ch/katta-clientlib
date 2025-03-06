/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows.exceptions;

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
