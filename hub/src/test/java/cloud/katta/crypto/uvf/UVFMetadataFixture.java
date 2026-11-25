/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;

import org.cryptomator.cryptolib.api.NameShortener;
import org.cryptomator.cryptolib.api.UvfKeyEncryptionKey;
import org.cryptomator.cryptolib.api.UvfLockedVault;
import org.cryptomator.cryptolib.api.UvfRecoveryKey;
import org.cryptomator.cryptolib.api.UvfUnlockedVault;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MultiDecrypter;
import com.nimbusds.jose.crypto.MultiEncrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;

/**
 * Vault metadata with custom fields as written by Hub. Workaround until cryptolib supports writing custom fields
 * when creating a vault.
 */
public final class UVFMetadataFixture {

    private static final String RECIPIENT_DETAILS = "recipientDetails";
    private static final String SPEC_VERSION_HEADER = "uvf.spec.version";

    private UVFMetadataFixture() {
    }

    /**
     * @param keys    Member key and recovery key
     * @param payload Custom fields to add, fields with null value are omitted
     * @return Contents of vault.uvf
     */
    public static String create(final HubVaultKeys keys, final UVFMetadataPayload payload) throws Exception {
        final UvfUnlockedVault vault = UvfUnlockedVault.create(FastSecureRandomProvider.get().provide(),
                UvfKeyEncryptionKey.memberKey(keys.memberKey().toSecretKey("AES")),
                UvfRecoveryKey.of(keys.recoveryKey().keyPair()), NameShortener.DISABLED);
        try {
            return withCustomFields(new String(vault.serializeConfig(), StandardCharsets.US_ASCII), keys.memberKey(), payload.toMap());
        }
        finally {
            vault.destroy();
        }
    }

    /**
     * Encrypt the payload again with the same protected header and recipients including the given fields.
     */
    private static String withCustomFields(final String metadata, final OctetSequenceKey memberKey, final Map<String, Object> fields) throws Exception {
        final JWEObjectJSON jwe = JWEObjectJSON.parse(metadata);
        final OctetSequenceKey kek = new OctetSequenceKey.Builder(memberKey)
                .keyID(UvfLockedVault.MEMBER_KEY_KID).algorithm(JWEAlgorithm.A256KW).build();
        jwe.decrypt(new MultiDecrypter(kek, Collections.singleton(SPEC_VERSION_HEADER)));
        final Map<String, Object> payload = jwe.getPayload().toJSONObject();
        fields.forEach((key, value) -> {
            if(value != null) {
                payload.put(key, value);
            }
        });
        @SuppressWarnings("unchecked") final Map<String, Object> recovery = (Map<String, Object>) ((Map<String, Object>) payload.get(RECIPIENT_DETAILS))
                .get(UvfLockedVault.RECOVERY_KEY_KID);
        @SuppressWarnings("unchecked") final ECKey recoveryKey = new ECKey.Builder(ECKey.parse((Map<String, Object>) recovery.get("jwk")))
                .keyID(UvfLockedVault.RECOVERY_KEY_KID).algorithm(JWEAlgorithm.parse((String) recovery.get("alg"))).build();
        final JWEObjectJSON encrypted = new JWEObjectJSON(jwe.getHeader(), new Payload(payload));
        encrypted.encrypt(new MultiEncrypter(new JWKSet(Arrays.<JWK>asList(kek, recoveryKey))));
        return encrypted.serializeGeneral();
    }
}
