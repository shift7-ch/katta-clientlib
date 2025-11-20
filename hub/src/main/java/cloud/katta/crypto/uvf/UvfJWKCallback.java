/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.exception.LoginCanceledException;
import ch.cyberduck.core.vault.JWKCallback;
import ch.cyberduck.core.vault.JWKCredentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWK;

public class UvfJWKCallback implements JWKCallback {

    private final JWK key;

    public UvfJWKCallback(final JWK key) throws JsonProcessingException {
        this.key = key;
    }

    @Override
    public void close(final String input) {
        //
    }

    @Override
    public JWKCredentials prompt(final Host bookmark, final String title, final String reason, final LoginOptions options) throws LoginCanceledException {
        return new JWKCredentials(key);
    }
}
