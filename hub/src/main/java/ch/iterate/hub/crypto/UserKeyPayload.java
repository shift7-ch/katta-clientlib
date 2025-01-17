/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto;


import java.util.Map;

import ch.iterate.hub.model.JWEPayload;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.Payload;

/**
 * Represents Cryptomator Hub <a href="https://docs.cryptomator.org/en/latest/security/hub/#user-key-pair>User Keys</a>.
 * Counterpart of <a href="https://github.com/cryptomator/hub/blob/develop/frontend/src/common/crypto.ts"><code>UserKeyPayload</code></a>.
 */
public class UserKeyPayload extends JWEPayload {

    private static final String JSON_PROPERTY_ECDH_PRIVATE_KEY = "ecdhPrivateKey";
    private static final String JSON_PROPERTY_ECDSA_PRIVATE_KEY = "ecdsaPrivateKey";

    @JsonProperty("key") // redundant for backwards compatibility
    String key;

    @JsonProperty(JSON_PROPERTY_ECDH_PRIVATE_KEY)
    String ecdhPrivateKey;

    @JsonProperty(JSON_PROPERTY_ECDSA_PRIVATE_KEY)
    String ecdsaPrivateKey;


    public UserKeyPayload() {
    }

    public UserKeyPayload(final String ecdhPrivateKey, final String ecdsaPrivateKey) {
        this.key = ecdhPrivateKey;
        this.ecdhPrivateKey = ecdhPrivateKey;
        this.ecdsaPrivateKey = ecdsaPrivateKey;
    }

    /**
     * @return base64-encoded ECDH private key in PKCS format.
     */
    public String ecdhPrivateKey() {
        return ecdhPrivateKey;
    }

    /**
     * @return base64-encoded ECDH private key in PKCS format.
     */
    public String ecdsaPrivateKey() {
        return ecdsaPrivateKey;
    }

    public static UserKeyPayload createFromPayload(final Payload payload) {
        final Map<String, Object> fields = payload.toJSONObject();
        return new UserKeyPayload((String) fields.get(JSON_PROPERTY_ECDH_PRIVATE_KEY), (String) fields.get(JSON_PROPERTY_ECDSA_PRIVATE_KEY));
    }
}
