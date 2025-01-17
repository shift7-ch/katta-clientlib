/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.model;


import java.security.interfaces.ECPublicKey;

import ch.iterate.hub.crypto.JWE;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

public class SetupCodeJWE extends JWEPayload {

    @JsonProperty("setupCode")
    String setupCode;


    public SetupCodeJWE(final String setupCode) {
        this.setupCode = setupCode;
    }

    public String encryptForUser(final ECPublicKey recipientPublicKey) throws JOSEException, JsonProcessingException {
        return JWE.ecdhEsEncrypt(this, "org.cryptomator.hub.userkey", recipientPublicKey);
    }
}
