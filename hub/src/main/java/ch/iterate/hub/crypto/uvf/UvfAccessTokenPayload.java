/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.uvf;


import org.openapitools.jackson.nullable.JsonNullableModule;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import ch.iterate.hub.crypto.JWE;
import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import ch.iterate.hub.model.JWEPayload;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetSequenceKey;

/**
 * Represents a user-specific access token containing the vault member key (vault members) and optionally the recoveryKey (vault admins only),
 * which allow to decrypt the <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault%20metadata/README.md"><code>vault.uvf</code> metadata</a>.
 * Counterpart of <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts"><code>UvfAccessTokenPayload</code></a>.
 */
public class UvfAccessTokenPayload extends JWEPayload {

    // member key aka. masterkey aka. vault key
    @JsonProperty("key")
    String key;

    // optional private key of the recovery key pair (PKCS8-encoded; only shared with vault owners)
    @JsonProperty("recoveryKey")
    String recoveryKey;

    public static UvfAccessTokenPayload fromJWE(String jwe) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper.readValue(jwe, UvfAccessTokenPayload.class);
    }

    @Override
    public String toString() {
        return "UvfAccessTokenPayload{" +
                "key='" + key +
                ", recoveryKey='" + recoveryKey +
                '}';
    }

    public UvfAccessTokenPayload() {
    }


    public String key() {
        return key;
    }

    public UvfAccessTokenPayload withKey(final String key) {
        this.key = key;
        return this;
    }

    public String recoveryKey() {
        return recoveryKey;
    }

    public UvfAccessTokenPayload withRecoveryKey(final String recoveryKey) {
        this.recoveryKey = recoveryKey;
        return this;
    }

    public OctetSequenceKey memberKeyRecipient() {
        return UvfMetadataPayload.UniversalVaultFormatJWKS.memberKeyFromRawKey(Base64.getDecoder().decode(key()));
    }

    public ECKey recoveryKeyRecipient(final ECKey publicRecoveryKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        return UvfMetadataPayload.UniversalVaultFormatJWKS.recoveryKeyFromRawKey(publicRecoveryKey, Base64.getDecoder().decode(recoveryKey()));
    }

    /**
     * Creates a user-specific access token for the given vault.
     *
     * @param userPublicKey the public key of the user
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts">universalVaultFormat.ts/UniversalVaultFormat.encryptForUser()</a>
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/OtherVaultMember.createAccessToken()</a>
     */
    public String encryptForUser(final ECPublicKey userPublicKey) throws JOSEException, JsonProcessingException {
        return JWE.ecdhEsEncrypt(this, "org.cryptomator.hub.userkey", userPublicKey);
    }
}
