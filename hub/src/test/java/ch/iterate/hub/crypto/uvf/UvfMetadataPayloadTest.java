/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.uvf;

import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;

import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;

import static ch.iterate.hub.crypto.KeyHelper.decodeKeyPair;
import static ch.iterate.hub.crypto.KeyHelper.decodePrivateKey;
import static org.junit.jupiter.api.Assertions.*;

class UvfMetadataPayloadTest {

    final ECKey alice = new ECKey.Builder(
            Curve.P_384,
            new Base64URL("j6Retxx-L-rURQ4WNc8LvoqjbdPtGS6n9pCJgcm1U-NAWuWEvwJ_qi2tlrv_4w4p"),
            new Base64URL("wS-Emo-Q9qdtkHMJiDfVDAaxhF2-nSkDRn2Eg9CbG0pVwGEpaDybx_YYJwIaYooO")
    )
            .d(new Base64URL("wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_"))
            .build();

    @Test
    public void serializePublicRecoverykey() throws JOSEException {
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final ECKey recoveryKey = jwks.recoveryKey();
        assertEquals(String.format("{\"keys\":[{\"kty\":\"EC\",\"crv\":\"P-384\",\"kid\":\"%s\",\"x\":\"%s\",\"y\":\"%s\",\"alg\":\"ECDH-ES+A256KW\"}]}", recoveryKey.getKeyID(), recoveryKey.getX(), recoveryKey.getY()), jwks.serializePublicRecoverykey());
    }

    @Test
    public void memberKeyToAccessTokenAndBack() throws JOSEException {
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final UvfAccessTokenPayload accessToken = jwks.toAccessToken();
        assertNull(accessToken.recoveryKey());
        assertEquals(jwks.memberKey(), accessToken.memberKeyRecipient());
    }

    @Test
    public void recoveryKeyToOwnerAccessTokenAndBack() throws JOSEException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final UvfAccessTokenPayload accessToken = jwks.toOwnerAccessToken();

        final JWKSet recoveredJwks = JWKSet.parse(jwks.serializePublicRecoverykey());
        assertEquals(1, recoveredJwks.getKeys().size());
        final ECKey publicRecoveryKey = (ECKey) recoveredJwks.getKeys().get(0);
        assertFalse(publicRecoveryKey.isPrivate());

        final ECKey recoveryKey = accessToken.recoveryKeyRecipient(publicRecoveryKey);
        assertEquals(jwks.recoveryKey().toECPrivateKey(), recoveryKey.toECPrivateKey());

        assertEquals(jwks.recoveryKey(), recoveryKey);
    }

    @Test
    public void encryptDecrypt() throws JOSEException, JsonProcessingException, ParseException {
        final byte[] rawMasterKey = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(rawMasterKey);
        final HashMap<String, String> keys = new HashMap<String, String>() {{
            put("key01", Base64URL.encode(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).toString());
            put("key02", Base64URL.encode(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).toString());
        }};
        final UvfMetadataPayload orig = new UvfMetadataPayload()
                .withFileFormat("AES-256-GCM-32k")
                .withNameFormat("AES-256-SIV")
                .withSeeds(keys)
                .withLatestSeed("key0")
                .withinitialSeed("key1")
                .withKdf("1STEP-HMAC-SHA512")
                .withStorage(
                        new VaultMetadataJWEBackendDto()
                                .provider("provider")
                                .nickname("nickname")
                                .defaultPath("bucket")
                ).withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                        .enabled(true)
                        .maxWotDepth(42)
                );

        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final OctetSequenceKey memberKey = jwks.memberKey();
        final ECKey recoveryKey = jwks.recoveryKey();

        final String encrypted = orig.encrypt("https://example.com/api/", UUID.randomUUID(), jwks.toJWKSet());

        // decrypt with memberKey
        {
            final UvfMetadataPayload decrypted = UvfMetadataPayload.decryptWithJWK(encrypted, memberKey);
            assertEquals(orig, decrypted);
        }

        // decrypt with recoveryKey
        {
            final UvfMetadataPayload decrypted = UvfMetadataPayload.decryptWithJWK(encrypted, recoveryKey);
            assertEquals(orig, decrypted);
        }

        // decryption fails with wrong key
        {
            final ECKey fake = new ECKey.Builder(recoveryKey).keyID("kiddo").build();
            assertThrows(JOSEException.class, () -> UvfMetadataPayload.decryptWithJWK(encrypted, fake));
        }
    }

    @Test
    public void decryptWithRecoveryKey() throws ParseException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException, JsonProcessingException {
        // https://datatracker.ietf.org/doc/html/rfc7516#section-7.2.1
        final String jwe = "{\"protected\":\"eyJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvVE9ETy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9\",\"recipients\":[{\"header\":{\"kid\":\"org.cryptomator.hub.memberkey\",\"alg\":\"A256KW\"},\"encrypted_key\":\"XLoNIWvDKQqaDurrGt7VK9s2aggSMir7fS4ZdBUxdTxceCOHndo4kA\"},{\"header\":{\"kid\":\"org.cryptomator.hub.recoverykey.v2nb-mGX4POKMWCQKOogMWTlAn7DDqEOjjEGCsPEeco\",\"alg\":\"ECDH-ES+A256KW\",\"epk\":{\"key_ops\":[],\"ext\":true,\"kty\":\"EC\",\"x\":\"j6Retxx-L-rURQ4WNc8LvoqjbdPtGS6n9pCJgcm1U-NAWuWEvwJ_qi2tlrv_4w4p\",\"y\":\"wS-Emo-Q9qdtkHMJiDfVDAaxhF2-nSkDRn2Eg9CbG0pVwGEpaDybx_YYJwIaYooO\",\"crv\":\"P-384\"},\"apu\":\"\",\"apv\":\"\"},\"encrypted_key\":\"iNGgybMqmiXn_lbKLMMTpg38i1f00O6Zj65d5nzsLw3hyzuylGWpvA\"}],\"iv\":\"Pfy90C9SSq2gJr6B\",\"ciphertext\":\"ogYR1pZN9k97zEgO9Fj3ePQramtaUdHWq95geXD7FH1oB6T7fEOvdU2AEGWOcbIbQihn-eOqG2_5oTol16O_nQ4HcDOJ9w4R9EdpByuWG-kVNh_fpWeQjIuH4kO-Rtbf05JRVG2jexWopbIA8uHuoiOXSNpSYPTzTKirp2hU7w3sE01zycsu06HiasUX-tKZH_hbyiUEdTlFFLcvKpRwnYOQf6QMw0uY1IbUTX1cJY9LO5SpD8bZFZOd6hg_Qnsdcq52I8KkZyxocgqdW7P5OSUrv5z8DCLMPdByEpaz9cCOzQQvtZwHxJy82O4vDAh89QA_AzfK8J7TI5zJRlTGQgrNhiaVBC85fN3tMSv8sLfJs7rC_5LiVW5ZeqbQ52sAZQw0lfwgGpMmxsdMzPoVOLD8OxvX\",\"tag\":\"3Jiv6kI4Qoso60T0dRv9vIlca-P4UFyHqh-TEZvargM\"}";
        final ECKey key = new ECKey.Builder(
                Curve.P_384,
                new Base64URL("j6Retxx-L-rURQ4WNc8LvoqjbdPtGS6n9pCJgcm1U-NAWuWEvwJ_qi2tlrv_4w4p"),
                new Base64URL("wS-Emo-Q9qdtkHMJiDfVDAaxhF2-nSkDRn2Eg9CbG0pVwGEpaDybx_YYJwIaYooO")
        )
                .privateKey(decodePrivateKey("MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDBFsqkCaynpvLJzQYw/PCF9UQSkkCfbv7gsfQs/qacqG7Wwbv12SSzXFe7RDOXtdFGhZANiAAR6mjT6C2nooxSNvhoqs158dCwx+YoeafzQlmBCg7MzNsUOCay5YjxWNQ68SlxPegHuaHMLWvxACrupggowBGyQi9HLWSFuPcQtJDtU5g4YWME9MrRUHgb3DdcWDrN/ylc="))
                .keyID("org.cryptomator.hub.recoverykey.v2nb-mGX4POKMWCQKOogMWTlAn7DDqEOjjEGCsPEeco")
                .build();

        final UvfMetadataPayload meta = UvfMetadataPayload.decryptWithJWK(jwe, key);
        assertEquals(meta.fileFormat(), "AES-256-GCM-32k");
        assertEquals(meta.nameFormat(), "AES-SIV-512-B64URL");
        assertEquals(meta.seeds().size(), 1);
        assertEquals(meta.seeds().get("ZO3G9w"), "p6zznin4zSGt7gH6T95_kZj6HndpyUdY-1QVfxR2k20");
        assertEquals(meta.initialSeed(), "ZO3G9w");
        assertEquals(meta.latestSeed(), "ZO3G9w");
        assertEquals(meta.kdf(), "HKDF-SHA512");
        assertEquals(meta.kdfSalt(), "pNxWJ5R5TO0mbkmL5pv7M3tAi6Etoh_SK73Q0KvfKMY");
        assertEquals(meta.automaticAccessGrant().getEnabled(), true);
        assertEquals(meta.automaticAccessGrant().getMaxWotDepth(), -1);
        assertNull(meta.storage());
    }

    @Test
    public void computeRootDirId() throws JOSEException, ParseException, JsonProcessingException, InvalidKeySpecException {
        final ECKeyPair ecKeyPair = decodeKeyPair(Base64.getEncoder().encodeToString(alice.toECPublicKey().getEncoded()), (Base64.getEncoder().encodeToString(alice.toECPrivateKey().getEncoded())));
        final UserKeys userKeys = new UserKeys(ecKeyPair, P384KeyPair.generate());

        final String uvf = "{\"protected\":\"eyJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvVE9ETy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9\",\"recipients\":[{\"header\":{\"kid\":\"org.cryptomator.hub.memberkey\",\"alg\":\"A256KW\"},\"encrypted_key\":\"7FtABJ5BpSM9Ft8wUPfLQc-12WF57kX0tWtRWiVwA_N_gJBa9iwhzw\"},{\"header\":{\"kid\":\"org.cryptomator.hub.recoverykey.1h24rxLxIlNRPQAn5NBP0fL3VKNTmqS6NEnt2clI5ko\",\"alg\":\"ECDH-ES+A256KW\",\"epk\":{\"key_ops\":[],\"ext\":true,\"kty\":\"EC\",\"x\":\"oNu46YFrgrGSvl98HyDD3_iPkfZBnpYgEHmPL3qbO4AdwBsycpIqcHwKhT8Lt7B8\",\"y\":\"P80VgJFml85v_F2-aPYdgDQX_DPGZr1s_p8gWF4Idkp13QfKdhi32C7Zoy5kzPWO\",\"crv\":\"P-384\"},\"apu\":\"\",\"apv\":\"\"},\"encrypted_key\":\"QaJn0TP7mGAc5ukOpZ0gNAuBtCW7hPCkj8Jp4bhMftQfJefHNyqE7Q\"}],\"iv\":\"Wgif0WP21-MAwvWs\",\"ciphertext\":\"-n5CePmmN99I4KqlnR64Fuu5b2Md9s4CGxLMm7KQqu65H0ug7Fs5HHnrx_gkpFiv1Mn-jwrkoEtiixyQcYX6UcoyT2dY1MkLQB7QU9mdMpZU3n19Q2sAx1-gfTCd7IzVXef7SEfuscdQL1QTKJW454Dy8L3WwPiDpUgt9ED7mMFdJ6lJ3_EFYstN0VFAVf_jwtIILmQrjkM_LI0FFKfqkOCH2nuE9xG8ihPH9X9OStllPp00G9_onYu9mrg-smiNNK2Ib19CZJ2E6mAp7F_LGiz6p203fsprj4XY9J6t8zl5Vpc61NmFvzvY4j3_5FpD_BmpVr8tyyVT9zqWn4vsBAHORQ1V_b9v68O7CekCebpQvpmzEPZwZN1Ma_T6oI7Ydn1rtBnDruVrpWm01RL8XpHnFbko\",\"tag\":\"vPLd65IEcexmhGbYPM0cYI53H4Pp1OfTaAq_QGrneLM\"}";
        final String accessToken = "eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoiUXZRWUpUd3dSVEg2MWRFS3ZoNDI4ZG9nN3pRTFFxY3I0NUhwZTRqZFQ5Qno2bjcyVzQ4dTJ3WXk0UXlyZ0kxciIsInkiOiJZS1RtQ04zZXNKNDJVbUpzLU44NTFKamsyUFVPUU0zZXpCTkJvZGk4RnRNUDlUeUhoXzc0aHpxTC1EYTZkMXlwIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.rdysEEQN0FidglDtK5yyaEpQtv4CsYLOQd__y7REkb_3BLP9nD4Blw.dFb9JOdveiw3LmIs.rSMkz8VoB_LspnvxvmRzCWNVLShTWfbzHfqe5lwrWwumYCdeRPM.xsS2tDUr2khJrLxHex8gZhBgO_CMA_PxFlR-ku3JiT8";

        final UvfAccessTokenPayload accessTokenDecrypted = userKeys.decryptAccessToken(accessToken);
        final UvfMetadataPayload meta = UvfMetadataPayload.decryptWithJWK(uvf, accessTokenDecrypted.memberKeyRecipient());
        assertEquals("24UBEDeGu5taq7U4GqyA0MXUXb9HTYS6p3t9vvHGJAc=", Base64.getEncoder().encodeToString(meta.computeRootDirId()));
    }

    @Test
    public void computeRootDirIdHash() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        final ECKeyPair ecKeyPair = decodeKeyPair(Base64.getEncoder().encodeToString(alice.toECPublicKey().getEncoded()), (Base64.getEncoder().encodeToString(alice.toECPrivateKey().getEncoded())));
        final UserKeys userKeys = new UserKeys(ecKeyPair, P384KeyPair.generate());

        final byte[] rootDirId = Base64.getDecoder().decode("24UBEDeGu5taq7U4GqyA0MXUXb9HTYS6p3t9vvHGJAc=");
        final String uvf = "{\"protected\":\"eyJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvVE9ETy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9\",\"recipients\":[{\"header\":{\"kid\":\"org.cryptomator.hub.memberkey\",\"alg\":\"A256KW\"},\"encrypted_key\":\"7FtABJ5BpSM9Ft8wUPfLQc-12WF57kX0tWtRWiVwA_N_gJBa9iwhzw\"},{\"header\":{\"kid\":\"org.cryptomator.hub.recoverykey.1h24rxLxIlNRPQAn5NBP0fL3VKNTmqS6NEnt2clI5ko\",\"alg\":\"ECDH-ES+A256KW\",\"epk\":{\"key_ops\":[],\"ext\":true,\"kty\":\"EC\",\"x\":\"oNu46YFrgrGSvl98HyDD3_iPkfZBnpYgEHmPL3qbO4AdwBsycpIqcHwKhT8Lt7B8\",\"y\":\"P80VgJFml85v_F2-aPYdgDQX_DPGZr1s_p8gWF4Idkp13QfKdhi32C7Zoy5kzPWO\",\"crv\":\"P-384\"},\"apu\":\"\",\"apv\":\"\"},\"encrypted_key\":\"QaJn0TP7mGAc5ukOpZ0gNAuBtCW7hPCkj8Jp4bhMftQfJefHNyqE7Q\"}],\"iv\":\"Wgif0WP21-MAwvWs\",\"ciphertext\":\"-n5CePmmN99I4KqlnR64Fuu5b2Md9s4CGxLMm7KQqu65H0ug7Fs5HHnrx_gkpFiv1Mn-jwrkoEtiixyQcYX6UcoyT2dY1MkLQB7QU9mdMpZU3n19Q2sAx1-gfTCd7IzVXef7SEfuscdQL1QTKJW454Dy8L3WwPiDpUgt9ED7mMFdJ6lJ3_EFYstN0VFAVf_jwtIILmQrjkM_LI0FFKfqkOCH2nuE9xG8ihPH9X9OStllPp00G9_onYu9mrg-smiNNK2Ib19CZJ2E6mAp7F_LGiz6p203fsprj4XY9J6t8zl5Vpc61NmFvzvY4j3_5FpD_BmpVr8tyyVT9zqWn4vsBAHORQ1V_b9v68O7CekCebpQvpmzEPZwZN1Ma_T6oI7Ydn1rtBnDruVrpWm01RL8XpHnFbko\",\"tag\":\"vPLd65IEcexmhGbYPM0cYI53H4Pp1OfTaAq_QGrneLM\"}";
        final String accessToken = "eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoiUXZRWUpUd3dSVEg2MWRFS3ZoNDI4ZG9nN3pRTFFxY3I0NUhwZTRqZFQ5Qno2bjcyVzQ4dTJ3WXk0UXlyZ0kxciIsInkiOiJZS1RtQ04zZXNKNDJVbUpzLU44NTFKamsyUFVPUU0zZXpCTkJvZGk4RnRNUDlUeUhoXzc0aHpxTC1EYTZkMXlwIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.rdysEEQN0FidglDtK5yyaEpQtv4CsYLOQd__y7REkb_3BLP9nD4Blw.dFb9JOdveiw3LmIs.rSMkz8VoB_LspnvxvmRzCWNVLShTWfbzHfqe5lwrWwumYCdeRPM.xsS2tDUr2khJrLxHex8gZhBgO_CMA_PxFlR-ku3JiT8";

        final UvfAccessTokenPayload accessTokenDecrypted = userKeys.decryptAccessToken(accessToken);
        final UvfMetadataPayload meta = UvfMetadataPayload.decryptWithJWK(uvf, accessTokenDecrypted.memberKeyRecipient());
        final String hash = meta.computeRootDirIdHash(rootDirId);
        assertEquals("6DYU3E5BTPAZ4DWEQPQK3AIHX2DXSPHG", hash);
    }
}
