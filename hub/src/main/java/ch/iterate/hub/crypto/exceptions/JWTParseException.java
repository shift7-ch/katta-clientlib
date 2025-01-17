/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.exceptions;

public class JWTParseException extends Exception {
    public JWTParseException(String message) {
        super(message);
    }
}