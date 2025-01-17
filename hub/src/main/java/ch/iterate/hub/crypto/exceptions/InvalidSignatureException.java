/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.exceptions;

public class InvalidSignatureException extends Exception {
    public InvalidSignatureException(String message) {
        super(message);
    }
}