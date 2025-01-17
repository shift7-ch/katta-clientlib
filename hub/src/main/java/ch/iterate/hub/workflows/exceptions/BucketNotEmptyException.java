/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows.exceptions;

public class BucketNotEmptyException extends Exception {
    public BucketNotEmptyException(String message) {
        super(message);
    }

    public BucketNotEmptyException(String message, Throwable cause) {
        super(message, cause);
    }

    public BucketNotEmptyException(Throwable cause) {
        super(cause);
    }
}
