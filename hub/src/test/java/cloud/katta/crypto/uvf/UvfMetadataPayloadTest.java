/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.cryptomator.UVFVault;
import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;

import org.cryptomator.cryptolib.api.UVFMasterkey;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;

import static cloud.katta.crypto.KeyHelper.decodePrivateKey;
import static org.junit.jupiter.api.Assertions.*;

import cloud.katta.crypto.exceptions.NotECKeyException;
import cloud.katta.protocols.hub.HubProtocol;
import cloud.katta.protocols.hub.HubSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;

class UvfMetadataPayloadTest {

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
        assertTrue(orig.toString().startsWith("UvfMetadataPayload{fileFormat='AES-256-GCM-32k', nameFormat='AES-256-SIV', seeds={key02=********, key01=********}, initialSeed='key1', latestSeed='key0', kdf='1STEP-HMAC-SHA512', kdfSalt='********', automaticAccessGrant=class AutomaticAccessGrant {"));
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
        assertEquals("AES-256-GCM-32k", meta.fileFormat());
        assertEquals("AES-SIV-512-B64URL", meta.nameFormat());
        assertEquals(1, meta.seeds().size());
        assertEquals("p6zznin4zSGt7gH6T95_kZj6HndpyUdY-1QVfxR2k20", meta.seeds().get("ZO3G9w"));
        assertEquals("ZO3G9w", meta.initialSeed());
        assertEquals("ZO3G9w", meta.latestSeed());
        assertEquals("HKDF-SHA512", meta.kdf());
        assertEquals("pNxWJ5R5TO0mbkmL5pv7M3tAi6Etoh_SK73Q0KvfKMY", meta.kdfSalt());
        assertEquals(true, meta.automaticAccessGrant().getEnabled());
        assertEquals(-1, meta.automaticAccessGrant().getMaxWotDepth());
        assertNull(meta.storage());
    }

    @Test
    public void testWorkaround() {
        // example of byte array -> UTF-8 -> byte array not working
        final byte[] rootDirId = Base64.getDecoder().decode("L3CoPPdXaaDgrM5YhBujn2t2LFTE5XjYUzC1htzk6tY=");
        assertFalse(Arrays.equals(rootDirId, new String(rootDirId, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
        // restricting to alphanumeric does work
        final byte[] rootDirId2 = new AlphanumericRandomStringService(4).random().getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(rootDirId2, new String(rootDirId2, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testUVFMasterkeyFromUvfMetadataPayload() throws JsonProcessingException {
        final UvfMetadataPayload uvmetadataPayload = UvfMetadataPayload.create();
        UVFMasterkey.fromDecryptedPayload(uvmetadataPayload.toJSON());
    }

    @Test
    public void testUvfVaultLoadFromMetadataPayload() throws JsonProcessingException, BackgroundException {
        final UvfMetadataPayload uvfMetadataPayload = UvfMetadataPayload.create();
        final UVFVault uvfVault = new UVFVault(new Path("/", EnumSet.of(AbstractPath.Type.directory)));
        uvfVault.load(new HubSession(new Host(new HubProtocol()), new DisabledX509TrustManager(), new DefaultX509KeyManager()),
                new UvfMetadataPayloadPasswordCallback(uvfMetadataPayload));
    }
}
