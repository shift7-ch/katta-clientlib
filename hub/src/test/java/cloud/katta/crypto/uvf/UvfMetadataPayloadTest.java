/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import ch.cyberduck.core.AbstractPath;
import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.TestProtocol;
import ch.cyberduck.core.cryptomator.impl.uvf.CryptoVault;
import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;

import org.cryptomator.cryptolib.api.UVFMasterkey;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;

import cloud.katta.crypto.exceptions.NotECKeyException;
import cloud.katta.protocols.hub.HubProtocol;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MultiEncrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import static org.junit.jupiter.api.Assertions.*;

class UvfMetadataPayloadTest {

    @Test
    void serializePublicRecoverykey() throws JOSEException {
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final ECKey recoveryKey = jwks.recoveryKey();
        assertEquals(String.format("{\"keys\":[{\"kty\":\"EC\",\"crv\":\"P-384\",\"kid\":\"%s\",\"x\":\"%s\",\"y\":\"%s\",\"alg\":\"ECDH-ES+A256KW\"}]}", recoveryKey.getKeyID(), recoveryKey.getX(), recoveryKey.getY()), jwks.serializePublicRecoveryKey());
    }

    @Test
    void memberKeyToAccessTokenAndBack() throws JOSEException {
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final UvfAccessTokenPayload accessToken = jwks.toAccessToken();
        assertNull(accessToken.recoveryKey());
        assertEquals(jwks.memberKey(), accessToken.memberKeyRecipient());
    }

    @Test
    void recoveryKeyToOwnerAccessTokenAndBack() throws JOSEException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final UvfAccessTokenPayload accessToken = jwks.toOwnerAccessToken();

        final JWKSet recoveredJwks = JWKSet.parse(jwks.serializePublicRecoveryKey());
        assertEquals(1, recoveredJwks.getKeys().size());
        final ECKey publicRecoveryKey = (ECKey) recoveredJwks.getKeys().get(0);
        assertFalse(publicRecoveryKey.isPrivate());

        final ECKey recoveryKey = accessToken.recoveryKeyRecipient(publicRecoveryKey);
        assertEquals(jwks.recoveryKey().toECPrivateKey(), recoveryKey.toECPrivateKey());

        assertEquals(jwks.recoveryKey(), recoveryKey);
    }

    @Test
    void encryptDecrypt() throws JOSEException, JsonProcessingException, ParseException, SecurityFailure {
        final byte[] rawMasterKey = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(rawMasterKey);
        final HashMap<String, String> keys = new HashMap<String, String>() {{
            put("key01", Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            put("key02", Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
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

        final UUID vaultId = UUID.randomUUID();
        final String encrypted = orig.encrypt("https://example.com/gateway/api/", vaultId, jwks.toJWKSet());

        // decrypt with memberKey
        {
            final UvfMetadataPayload decrypted = UvfMetadataPayload.decryptWithJWK(encrypted, memberKey);
            assertEquals(String.format("https://example.com/gateway/api/vaults/%s", vaultId), JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("cloud.katta.origin"));
            assertEquals(orig, decrypted);
        }

        // decrypt with recoveryKey
        {
            final UvfMetadataPayload decrypted = UvfMetadataPayload.decryptWithJWK(encrypted, recoveryKey);
            assertEquals(String.format("https://example.com/gateway/api/vaults/%s", vaultId), JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("cloud.katta.origin"));
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
    void testDecrypt() throws ParseException, JOSEException, JsonProcessingException, SecurityFailure {
        final boolean regenerate = false;
        if(regenerate) {
            final JWKSet jwks = UvfMetadataPayload.createKeys().toJWKSet();
            System.out.println(jwks.getKeys());
            final HashMap<String, String> keys = new HashMap<String, String>() {{
                put("ZO3G9w", "p6zznin4zSGt7gH6T95_kZj6HndpyUdY-1QVfxR2k20");
            }};
            final UvfMetadataPayload orig = new UvfMetadataPayload()
                    .withFileFormat("AES-256-GCM-32k")
                    .withNameFormat("AES-SIV-512-B64URL")
                    .withSeeds(keys)
                    .withLatestSeed("ZO3G9w")
                    .withinitialSeed("ZO3G9w")
                    .withKdf("1STEP-HMAC-SHA512")
                    .withKdfSalt("pNxWJ5R5TO0mbkmL5pv7M3tAi6Etoh_SK73Q0KvfKMY")
                    .withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                            .enabled(true)
                            .maxWotDepth(-1)
                    );
            final String jwe = orig.encrypt("https://example.com/gateway/api/", UUID.randomUUID(), jwks);
            System.out.println(jwe);
        }
        // https://datatracker.ietf.org/doc/html/rfc7516#section-7.2.1
        final JWKSet jwks = JWKSet.parse("{\"keys\":[{\"kty\":\"oct\",\"kid\":\"org.cryptomator.hub.memberkey\",\"k\":\"S-0XJTj-1jjDo3_AzQeHzdQChbnf45Hq5LVjlNo83_0\",\"alg\":\"A256KW\"}, {\"kty\":\"EC\",\"d\":\"tkZalZZF_QI_9dJTrEvubpuFvTGMf-T5C7vLPygcLE2i6IvH7fPkopcWg3GGfBBP\",\"crv\":\"P-384\",\"kid\":\"org.cryptomator.hub.recoverykey.sE1E3aQQSBocn4oOJkB1UGgdETAys_Tqc7EaAXzsaYE\",\"x\":\"1vkEQ5cYoKrwniiEynSWuvtLiOOXika3b11KvkOTNZ2GQX8bdkoHcLYHMKv80Aan\",\"y\":\"bYITWQU6y5-_zSuMBxWRjNg0-rRtZG_dxtkeCXv39lKzTn5M08yWovJCf5pq1PPA\",\"alg\":\"ECDH-ES+A256KW\"}]}");
        final String jwe = "{\"ciphertext\":\"9z4eT9Hlmr2xQ71vkk2pAsDCytLXbOowsVV8ElKMmF3Gu4GIN9w5CeHFbykCvzF3gKtnOxeab0dQjs8pQR0EBHUS0DDuYMd9mSm86b3laR8Ol3spqFHFM1lwDZIeXDW2QcCgc9OopRmVdd23EwizkWCqymTl6u26C4NImwnpmjEjLZE71GebHkNDL6fkRVVjoUpQLbyWxnOksJZrl3vk8hFAJ315T5GwW6Sch3oJ7n0XJ31buyqurH7PfFLucKnUXNjPaKIpC5PclwTY3ITmcijjB9JiKBVemn8v1qB4s9YnFzu5hE8LWgH5ULYfoB_W2K-f_WRLVRGxmyKJiHtZLlG61zSrTM68Y31rYwmU2dyhwxV2vW3ejVjIe3576jbs_2-LQYSaMK8dDf1UN_1AC2ZqK3JZPJ7czieSARi34UzJRJv_mm8O6jiOVbNnPItQqfvbYqBZ_7Pg2SAreQLbW0s8TjIBiQ\",\"protected\":\"eyJ1dmYuc3BlYy52ZXJzaW9uIjoxLCJjdHkiOiJqc29uIiwiZW5jIjoiQTI1NkdDTSIsImNyaXQiOlsidXZmLnNwZWMudmVyc2lvbiJdLCJqa3UiOiJqd2tzLmpzb24iLCJjbG91ZC5rYXR0YS5vcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2dhdGV3YXkvYXBpL3ZhdWx0cy9iZWFkMGE5YS05OTExLTRhYjMtYTg1NS1jMWVmOWE2MDZiN2YifQ\",\"recipients\":[{\"encrypted_key\":\"K9e--5JvL_aHe2ah6YlseppMj3sVmYf7Bs_c6B5_msJtcPoYNWoIig\",\"header\":{\"alg\":\"A256KW\",\"kid\":\"org.cryptomator.hub.memberkey\"}},{\"encrypted_key\":\"4Wqu8rlteh43_l7Srn3NeT0hmUxhrnQ3ZCvOHADwTuE8BGNxgSE23A\",\"header\":{\"epk\":{\"kty\":\"EC\",\"crv\":\"P-384\",\"x\":\"_FWuKlyNa-LEizdA2brRuJ07YuBVciQ9x-ppGnCfKWjx-8_RNlvKxU5S5nHBbgdl\",\"y\":\"mQa_oSkYkKsXEIaiVSi-pZz4b9K-UZIF7kb3is5QEGJxXx9yRNA5p6a6unTT2B7G\"},\"alg\":\"ECDH-ES+A256KW\",\"kid\":\"org.cryptomator.hub.recoverykey.sE1E3aQQSBocn4oOJkB1UGgdETAys_Tqc7EaAXzsaYE\"}}],\"tag\":\"oFTc9Jn_amxoquVIlQ3pYg\",\"iv\":\"qN0j4ybzp-hUJIvW\"}";
        final String protectedDecode = new String(Base64.getDecoder().decode("eyJ1dmYuc3BlYy52ZXJzaW9uIjoxLCJjdHkiOiJqc29uIiwiZW5jIjoiQTI1NkdDTSIsImNyaXQiOlsidXZmLnNwZWMudmVyc2lvbiJdLCJqa3UiOiJqd2tzLmpzb24iLCJjbG91ZC5rYXR0YS5vcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2dhdGV3YXkvYXBpL3ZhdWx0cy9iZWFkMGE5YS05OTExLTRhYjMtYTg1NS1jMWVmOWE2MDZiN2YifQ"), StandardCharsets.UTF_8);
        assertEquals("{\"uvf.spec.version\":1,\"cty\":\"json\",\"enc\":\"A256GCM\",\"crit\":[\"uvf.spec.version\"],\"jku\":\"jwks.json\",\"cloud.katta.origin\":\"https://example.com/gateway/api/vaults/bead0a9a-9911-4ab3-a855-c1ef9a606b7f\"}", protectedDecode);
        for(JWK key : jwks.getKeys()) {
            final UvfMetadataPayload meta = UvfMetadataPayload.decryptWithJWK(jwe, key);
            assertEquals("AES-256-GCM-32k", meta.fileFormat());
            assertEquals("AES-SIV-512-B64URL", meta.nameFormat());
            assertEquals(1, meta.seeds().size());
            assertEquals("p6zznin4zSGt7gH6T95_kZj6HndpyUdY-1QVfxR2k20", meta.seeds().get("ZO3G9w"));
            assertEquals("ZO3G9w", meta.initialSeed());
            assertEquals("ZO3G9w", meta.latestSeed());
            assertEquals("1STEP-HMAC-SHA512", meta.kdf());
            assertEquals("pNxWJ5R5TO0mbkmL5pv7M3tAi6Etoh_SK73Q0KvfKMY", meta.kdfSalt());
            assertEquals(true, meta.automaticAccessGrant().getEnabled());
            assertEquals(-1, meta.automaticAccessGrant().getMaxWotDepth());
            assertNull(meta.storage());
        }
    }

    @Test
    void testMissingSpecVersion() throws JOSEException, JsonProcessingException, SecurityFailure, ParseException {
        final JWKSet jwks = UvfMetadataPayload.createKeys().toJWKSet();
        // header without additional critical param
        final JWEHeader header = new JWEHeader.Builder(EncryptionMethod.A256GCM)
                .jwkURL(URI.create("jwks.json"))
                .contentType("json")
                .build();
        final Payload payload = new Payload(new HashMap<String, Object>() {
        });
        final JWEObjectJSON builder = new JWEObjectJSON(header, payload);
        builder.encrypt(new MultiEncrypter(jwks));
        String jwe = builder.serializeGeneral();
        final SecurityFailure exc = assertThrows(SecurityFailure.class, () -> UvfMetadataPayload.decryptWithJWK(jwe, jwks.getKeyByKeyId("org.cryptomator.hub.memberkey")));
        assertEquals("Unexpected value for critical header uvf.spec.version: found null, expected \"1\"", exc.getMessage());
    }

    @Test
    void testWorkaround() {
        // example of byte array -> UTF-8 -> byte array not working
        final byte[] rootDirId = Base64.getDecoder().decode("L3CoPPdXaaDgrM5YhBujn2t2LFTE5XjYUzC1htzk6tY=");
        assertFalse(Arrays.equals(rootDirId, new String(rootDirId, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
        // restricting to alphanumeric does work
        final byte[] rootDirId2 = new AlphanumericRandomStringService(4).random().getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(rootDirId2, new String(rootDirId2, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testUVFMasterkeyFromUvfMetadataPayload() throws JsonProcessingException {
        final UvfMetadataPayload uvmetadataPayload = UvfMetadataPayload.create();
        UVFMasterkey.fromDecryptedPayload(uvmetadataPayload.toJSON());
    }

    @Test
    void testUvfVaultLoadFromMetadataPayload() throws JsonProcessingException, BackgroundException, JOSEException {
        final UvfMetadataPayload uvfMetadataPayload = UvfMetadataPayload.create();
        final UvfMetadataPayload.UniversalVaultFormatJWKS keys = UvfMetadataPayload.createKeys();
        final UUID vaultId = UUID.randomUUID();
        final VaultIdMetadataUVFProvider provider = new VaultIdMetadataUVFProvider(new Host(new TestProtocol()), vaultId, keys, uvfMetadataPayload);
        final CryptoVault uvfVault = new CryptoVault(new Path("/", EnumSet.of(AbstractPath.Type.directory)));
        final Host host = new Host(new HubProtocol());
        uvfVault.load(new HubSession(host, new DisabledX509TrustManager(), new DefaultX509KeyManager()),
                new UvfJWKCallback(keys.memberKey()), provider);
    }
}
