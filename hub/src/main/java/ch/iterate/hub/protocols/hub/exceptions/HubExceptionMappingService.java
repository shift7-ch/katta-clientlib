/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.protocols.hub.exceptions;

import ch.cyberduck.core.AbstractExceptionMappingService;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.DefaultSocketExceptionMappingService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.http.DefaultHttpResponseExceptionMappingService;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;

import ch.iterate.hub.client.ApiException;

public class HubExceptionMappingService extends AbstractExceptionMappingService<ApiException> {
    private static final Logger log = LogManager.getLogger(HubExceptionMappingService.class);

    @Override
    public BackgroundException map(final String message, final ApiException failure, final Path file) {
        return super.map(message, failure, file);
    }

    @Override
    public BackgroundException map(final ApiException failure) {
        for(final Throwable cause : ExceptionUtils.getThrowableList(failure)) {
            if(cause instanceof SocketException) {
                // Map Connection has been shutdown: javax.net.ssl.SSLException: java.net.SocketException: Broken pipe
                return new DefaultSocketExceptionMappingService().map((SocketException) cause);
            }
            if(cause instanceof HttpResponseException) {
                return new DefaultHttpResponseExceptionMappingService().map((HttpResponseException) cause);
            }
            if(cause instanceof IOException) {
                return new DefaultIOExceptionMappingService().map((IOException) cause);
            }
            if(cause instanceof IllegalStateException) {
                // Caused by: ch.cyberduck.core.sds.io.swagger.client.ApiException: javax.ws.rs.ProcessingException: java.lang.IllegalStateException: Connection pool shut down
                return new ConnectionCanceledException(cause);
            }
        }
        final StringBuilder buffer = new StringBuilder(failure.getMessage());
        return new DefaultHttpResponseExceptionMappingService().map(failure, buffer, failure.getCode());
    }
}
