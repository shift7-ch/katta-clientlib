/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;


import org.cryptomator.cryptolib.common.P384KeyPair;
import org.openapitools.jackson.nullable.JsonNullableModule;

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
 * Represents a user-specific access token containing the vault member key (vault members) and optionally the recoveryKey (vault admins only),
 * which allow to decrypt the <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault%20metadata/README.md"><code>vault.uvf</code> metadata</a>.
 * Counterpart of <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts"><code>UvfAccessTokenPayload</code></a>.
 */
public class UVFAccessTokenPayload extends JWEPayload {

    /**
     * Member key aka. masterkey aka. vault key
     * Represents a unique identifier for a member in the system.
     * <p>
     * This field is used as a key to associate a member with their relevant data.
     * It is serialized and deserialized to/from JSON as "key".
     */
    @JsonProperty("key")
    String memberKey;

    /**
     * Optional private key of the recovery key pair (PKCS8-encoded; only shared with vault owners)
     * <p>
     * The recovery key associated with the token payload.
     * This key is primarily used for backup or recovery purposes, enabling secure
     * access restoration in case of lost credentials or other applicable scenarios.
     * The recovery key is typically stored or transmitted securely to prevent unauthorized access.
     * <p>
     * This field is serialized/deserialized with the property name "recoveryKey".
     */
    @JsonProperty("recoveryKey")
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

    public UVFAccessTokenPayload(final String memberKey, final String recoveryKey) {
        this.memberKey = memberKey;
        this.recoveryKey = recoveryKey;
    }

    /**
     * Retrieves the key associated with a specific member.
     *
     * @return the member key as a String
     */
    public String key() {
        return memberKey;
    }

    /**
     * Retrieves the recovery key associated with this object.
     *
     * @return the recovery key as a String
     */
    public String recoveryKey() {
        return recoveryKey;
    }

    /**
     * @param userPublicKey the public key of the user
     * @return Creates a user-specific access token for the given vault.
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts">universalVaultFormat.ts/UniversalVaultFormat.encryptForUser()</a>
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/OtherVaultMember.createAccessToken()</a>
     */
    public String encryptForUser(final ECPublicKey userPublicKey) throws JOSEException, JsonProcessingException {
        return JWE.ecdhEsEncrypt(this, "org.cryptomator.hub.userkey", userPublicKey);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final UVFAccessTokenPayload that = (UVFAccessTokenPayload) o;
        return memberKey.equals(that.memberKey) && Objects.equals(recoveryKey, that.recoveryKey);
    }

    @Override
    public int hashCode() {
        int result = memberKey.hashCode();
        result = 31 * result + Objects.hashCode(recoveryKey);
        return result;
    }
}
