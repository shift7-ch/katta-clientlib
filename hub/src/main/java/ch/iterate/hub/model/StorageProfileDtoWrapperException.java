/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.model;

public class StorageProfileDtoWrapperException extends Exception {
    public StorageProfileDtoWrapperException(String message) {
        super(message);
    }

    public StorageProfileDtoWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageProfileDtoWrapperException(Throwable cause) {
        super(cause);
    }
}
