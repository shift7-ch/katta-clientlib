/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.wot;

import java.util.Objects;

import cloud.katta.client.model.UserDto;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.Payload;

/**
 * Signed Keys in WoT signature chain.
 * Counterpart of @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts"><code>SignedKeys</code></a>
 */
public class SignedKeys {
    private final String ecdhPublicKey;
    private final String ecdsaPublicKey;

    public SignedKeys(final String ecdhPublicKey, final String ecdsaPublicKey) {
        this.ecdhPublicKey = ecdhPublicKey;
        this.ecdsaPublicKey = ecdsaPublicKey;
    }

    public String ecdhPublicKey() {
        return ecdhPublicKey;
    }

    public String ecdsaPublicKey() {
        return ecdsaPublicKey;
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        final SignedKeys that = (SignedKeys) o;
        return Objects.equals(ecdhPublicKey, that.ecdhPublicKey) && Objects.equals(ecdsaPublicKey, that.ecdsaPublicKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(ecdhPublicKey);
        result = 31 * result + Objects.hashCode(ecdsaPublicKey);
        return result;
    }

    public static SignedKeys fromPayload(final Payload payload) {
        return new SignedKeys((String) payload.toJSONObject().get("ecdhPublicKey"), (String) payload.toJSONObject().get("ecdsaPublicKey"));
    }

    public static SignedKeys fromUser(final UserDto trustedUser) {
        return new SignedKeys(trustedUser.getEcdhPublicKey(), trustedUser.getEcdsaPublicKey());
    }

    public Payload toPayload() {
        return new Payload(new ImmutableMap.Builder<String, Object>().put("ecdhPublicKey", ecdhPublicKey).put("ecdsaPublicKey", ecdsaPublicKey).build());
    }

    @Override
    public String toString() {
        return "SignedKeys{" +
                "ecdhPublicKey='" + ecdhPublicKey + '\'' +
                ", ecdsaPublicKey='" + ecdsaPublicKey + '\'' +
                '}';
    }
}
