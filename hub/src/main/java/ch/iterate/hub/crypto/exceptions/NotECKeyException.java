/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.exceptions;

public class NotECKeyException extends Exception {
    public NotECKeyException(String message) {
        super(message);
    }

    public NotECKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotECKeyException(Throwable cause) {
        super(cause);
    }
}
