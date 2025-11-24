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
        final String encrypted = orig.encrypt("https://example.com/api/", vaultId, jwks.toJWKSet());

        // decrypt with memberKey
        {
            final UvfMetadataPayload decrypted = UvfMetadataPayload.decryptWithJWK(encrypted, memberKey);
            assertEquals(String.format("https://example.com/api/vaults/%s/uvf/vault.uvf", vaultId), JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("origin"));
            assertEquals(orig, decrypted);
        }

        // decrypt with recoveryKey
        {
            final UvfMetadataPayload decrypted = UvfMetadataPayload.decryptWithJWK(encrypted, recoveryKey);
            assertEquals(String.format("https://example.com/api/vaults/%s/uvf/vault.uvf", vaultId), JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("origin"));
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
            final String jwe = orig.encrypt("https://example.com/api/", UUID.randomUUID(), jwks);
            System.out.println(jwe);
        }
        // https://datatracker.ietf.org/doc/html/rfc7516#section-7.2.1
        final JWKSet jwks = JWKSet.parse("{\"keys\":[{\"kty\":\"oct\",\"kid\":\"org.cryptomator.hub.memberkey\",\"k\":\"4WJi6f_9KaZJyIJy4HYyMUGfYjehtaqi_ak_gpIj5XY\",\"alg\":\"A256KW\"}, {\"kty\":\"EC\",\"d\":\"1bGYxmHaw7oXv6Nc63FNvHn6FOyygMtQcIK81UmMIWraUg9AYYgAuPpIRpacPeV5\",\"crv\":\"P-384\",\"kid\":\"org.cryptomator.hub.recoverykey.6K50z0nLy9DZU9Cv-NsFfI7HaIGaj5maHRP9EKAMDDs\",\"x\":\"YY-auvFsCGBE5N6_34E2pM9AaX2CYNJNCCvUUSfZFjQ4wVh6BNKIZMBYF1aJ3U3y\",\"y\":\"805pQYnmWllxBLchD4cfBad0CcCu7NeIBbW_T-u8movgbsPeDieJhWIkDtu0E9vl\",\"alg\":\"ECDH-ES+A256KW\"}]\n}\n");
        final String jwe = "{\"ciphertext\":\"-hKvmtaa_lM-MLpDcs9jo_Iz2YMix9zOcvRvlPrLwmeU9C8v3qzNINlP3vxbDBq_NHNeQOqXXmv9GUxuGI89Kvqnw1HXoN1NxaDMpr867srI4dTt0UtOPvUEn-iBtRT1C7OawX0MSGdRQBVQIQl1zP1NNJYzJAp0Q9gl_37g7zRf3PBtXGcw2jzXhvh3Om9P_EP7PQjSOlnXCNmsdwoOLbR68OPO56U_YxVvjmhU16xcH9bEpRC_Pw532UbNF9w-g0NOX_AvMY4ZxiecQ9METzVygHkheSzR6H21MEeoRzd_XUpwVeJUytwAs4iU4kzUhhnyw1aq-kb4_GPNUHjZgs7PbMTKwqSaNMF1Xr5CXZ6dTP6H3ivEl4l7P8ulOvple9Fu3MSBU2X0QxGfpw2AtkO4x4-rNSmppJ_4AshHFOh3akf4n32R9LiFrXaLghw1-rTv3GbeAJXOqBrUZm40dGvDXvJfBw\",\"protected\":\"eyJ1dmYuc3BlYy52ZXJzaW9uIjoxLCJjdHkiOiJqc29uIiwiZW5jIjoiQTI1NkdDTSIsImNyaXQiOlsidXZmLnNwZWMudmVyc2lvbiJdLCJqa3UiOiJqd2tzLmpzb24iLCJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvYjU3NjRkMWItMzI2MS00ODkzLTkyMDQtYmFhNjEwNGVmMTZjL3V2Zi92YXVsdC51dmYifQ\",\"recipients\":[{\"encrypted_key\":\"_vzpGTPsvcladSI9ZcqT-7oa76pzUIGE078J6ZyZjLhtPpQG0AKELQ\",\"header\":{\"alg\":\"A256KW\",\"kid\":\"org.cryptomator.hub.memberkey\"}},{\"encrypted_key\":\"vq-gLpElx2kzfHO2fw8p7xXPzjbanuYK1YH8j71TemUHdKZK2yWtjw\",\"header\":{\"epk\":{\"kty\":\"EC\",\"crv\":\"P-384\",\"x\":\"gH8Qtn-p6PLDPqnLZa4jXp9Lq-Dn58UN0rjXoyTmUgW-eYQN4z6TyOYhBU5kW6Jv\",\"y\":\"XLxmLfpNNeqRnMFfP18XVP6QGzHNx-f0FvYeSmb2miWMpTxWZU9GBL31UGSJYdN3\"},\"alg\":\"ECDH-ES+A256KW\",\"kid\":\"org.cryptomator.hub.recoverykey.6K50z0nLy9DZU9Cv-NsFfI7HaIGaj5maHRP9EKAMDDs\"}}],\"tag\":\"2AcyWwCk0JeDdJr_gwu95w\",\"iv\":\"KBFeW9-gmou_impc\"}\n";
        final String protectedDecode = new String(Base64.getDecoder().decode("eyJ1dmYuc3BlYy52ZXJzaW9uIjoxLCJjdHkiOiJqc29uIiwiZW5jIjoiQTI1NkdDTSIsImNyaXQiOlsidXZmLnNwZWMudmVyc2lvbiJdLCJqa3UiOiJqd2tzLmpzb24iLCJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvYjU3NjRkMWItMzI2MS00ODkzLTkyMDQtYmFhNjEwNGVmMTZjL3V2Zi92YXVsdC51dmYifQ"), StandardCharsets.UTF_8);
        assertEquals("{\"uvf.spec.version\":1,\"cty\":\"json\",\"enc\":\"A256GCM\",\"crit\":[\"uvf.spec.version\"],\"jku\":\"jwks.json\",\"origin\":\"https://example.com/api/vaults/b5764d1b-3261-4893-9204-baa6104ef16c/uvf/vault.uvf\"}", protectedDecode);
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
