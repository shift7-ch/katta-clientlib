/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows.exceptions;

public class FirstLoginDeviceSetupException extends Exception {
    public FirstLoginDeviceSetupException(String message) {
        super(message);
    }

    public FirstLoginDeviceSetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirstLoginDeviceSetupException(Throwable cause) {
        super(cause);
    }
}
