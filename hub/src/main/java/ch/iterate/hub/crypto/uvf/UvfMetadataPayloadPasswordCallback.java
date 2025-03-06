/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.uvf;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.vault.VaultCredentials;

import com.fasterxml.jackson.core.JsonProcessingException;

public class UvfMetadataPayloadPasswordCallback extends DisabledPasswordCallback {

    private final UvfMetadataPayload payload;

    public UvfMetadataPayloadPasswordCallback(final UvfMetadataPayload payload) {
        this.payload = payload;
    }

    @Override
    public Credentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
        try {
            return new VaultCredentials(payload.toJSON());
        }
        catch(JsonProcessingException e) {
            throw new LoginCanceledException(e);
        }
    }
}
