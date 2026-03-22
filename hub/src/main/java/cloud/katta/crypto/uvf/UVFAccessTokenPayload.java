/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;


import org.cryptomator.cryptolib.common.P384KeyPair;
import org.openapitools.jackson.nullable.JsonNullableModule;

import javax.annotation.Nullable;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Objects;

import cloud.katta.crypto.JWE;
import cloud.katta.model.JWEPayload;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.OctetSequenceKey;

/**
 * JWE payload for UVF access tokens containing Base64 encoded member key and optionally recovery key required to decrypt vault metadata.
 *
 * @see <a href="https://github.com/encryption-alliance/unified-vault-format">Unified Vault Format</a>
 */
public class UVFAccessTokenPayload extends JWEPayload {

    /**
     * Base64 encoded member key
     * <p>
     * It is serialized and deserialized to/from JSON as "key".
     */
    @JsonProperty("key")
    String memberKey;

    /**
     * Optional Base64 encoded private key of the recovery key pair
     * <p>
     * This field is serialized/deserialized with the property name "recoveryKey".
     */
    @JsonProperty("recoveryKey")
    @Nullable
    String recoveryKey;

    public static UVFAccessTokenPayload fromJWE(final String jwe) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper.readValue(jwe, UVFAccessTokenPayload.class);
    }

    public UVFAccessTokenPayload() {
        this.memberKey = null;
        this.recoveryKey = null;
    }

    public UVFAccessTokenPayload(final OctetSequenceKey memberKey) {
        this.memberKey = Base64.getEncoder().encodeToString(memberKey.toByteArray());
        this.recoveryKey = null;
    }

    public UVFAccessTokenPayload(final OctetSequenceKey memberKey, final P384KeyPair recoveryKey) {
        this.memberKey = Base64.getEncoder().encodeToString(memberKey.toByteArray());
        this.recoveryKey = Base64.getEncoder().encodeToString(recoveryKey.getPrivate().getEncoded());
    }

    public UVFAccessTokenPayload(final String memberKey, @Nullable final String recoveryKey) {
        this.memberKey = memberKey;
        this.recoveryKey = recoveryKey;
    }

    /**
     * @return Base64 encoded member key
     */
    public String key() {
        return memberKey;
    }

    /**
     * Retrieves the recovery key associated with this object.
     *
     * @return the recovery key as a String
     */
    @Nullable
    public String recoveryKey() {
        return recoveryKey;
    }

    /**
     * Encrypts the access token for a given user.
     *
     * @param userPublicKey the public key of the user
     * @return User-specific access token for the given vault.
     */
    public String encryptForUser(final ECPublicKey userPublicKey) throws JOSEException, JsonProcessingException {
        return JWE.ecdhEsEncrypt(this, "org.cryptomator.hub.userkey", userPublicKey);
    }

    @Override
    public boolean equals(final Object o) {
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final UVFAccessTokenPayload that = (UVFAccessTokenPayload) o;
        return Objects.equals(memberKey, that.memberKey) && Objects.equals(recoveryKey, that.recoveryKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(memberKey);
        result = 31 * result + Objects.hashCode(recoveryKey);
        return result;
    }
}
