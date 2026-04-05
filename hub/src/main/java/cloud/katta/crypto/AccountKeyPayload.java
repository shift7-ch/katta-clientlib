/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import java.security.interfaces.ECPublicKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

public class AccountKeyPayload extends JWEPayload {

    @JsonProperty("setupCode")
    String setupCode;

    public AccountKeyPayload(final String setupCode) {
        this.setupCode = setupCode;
    }

    /**
     * @return Encrypts the setup code for a specific user.
     */
    public String encryptForUser(final ECPublicKey recipientPublicKey) throws JOSEException, JsonProcessingException {
        return JWE.ecdhEsEncrypt(this, "org.cryptomator.hub.userkey", recipientPublicKey);
    }
}
