/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.cryptomator.impl.uvf.UVFVault;
import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataAutomaticAccessGrantDto;
import cloud.katta.crypto.uvf.VaultMetadataStorageDto;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MultiEncrypter;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;

import static org.junit.jupiter.api.Assertions.*;

class HubVaultMetadataUVFProviderTest {

    @Test
    void testEncryptDecrypt() throws Exception {
        final byte[] rawMasterKey = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(rawMasterKey);
        final HashMap<String, String> keys = new HashMap<String, String>() {{
            put("key01", Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            put("key02", Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        }};
        final UVFMetadataPayload orig = new UVFMetadataPayload()
                .withFileFormat("AES-256-GCM-32k")
                .withNameFormat("AES-256-SIV")
                .withSeeds(keys)
                .withLatestSeed("key0")
                .withinitialSeed("key1")
                .withKdf("1STEP-HMAC-SHA512")
                .withStorage(
                        new VaultMetadataStorageDto()
                                .provider("provider")
                                .nickname("nickname")
                                .defaultPath("bucket")
                ).withAutomaticAccessGrant(new VaultMetadataAutomaticAccessGrantDto()
                        .enabled(true)
                        .maxWotDepth(42)
                );

        final HubVaultKeys jwks = HubVaultKeys.create();
        final OctetSequenceKey memberKey = jwks.memberKey();
        final Base64URL thumbprint = new ECKey.Builder(Curve.P_384, jwks.recoveryKey().getPublic()).build().computeThumbprint();
        final ECKey recoveryKey = ((ECKey) jwks.serialize().getKeyByKeyId(String.format(HubVaultKeys.KID_RECOVERY_KEY_PREFIX, thumbprint)));

        final UUID vaultId = UUID.randomUUID();

        final String encrypted = new String(new HubVaultMetadataUVFProvider(orig, "https://example.com/gateway/api/", vaultId, jwks.serialize()).encrypt(),
                StandardCharsets.US_ASCII);

        // decrypt with memberKey
        {
            final UVFMetadataPayload decrypted = new HubVaultMetadataUVFProvider(orig, "https://example.com/gateway/api/", vaultId,
                    new JWKSet(memberKey)).getPayload();
            assertEquals(String.format("https://example.com/gateway/api/vaults/%s/uvf/vault.uvf", vaultId),
                    JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("cloud.katta.origin"));
            assertEquals(orig, decrypted);
        }

        // decrypt with recoveryKey
        {
            final UVFMetadataPayload decrypted = new HubVaultMetadataUVFProvider(orig, "https://example.com/gateway/api/", vaultId,
                    new JWKSet(recoveryKey)).getPayload();
            assertEquals(String.format("https://example.com/gateway/api/vaults/%s/uvf/vault.uvf", vaultId),
                    JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("cloud.katta.origin"));
            assertEquals(orig, decrypted);
        }

        // decryption fails with wrong key
        {
            final ECKey fake = new ECKey.Builder(recoveryKey).keyID("kiddo").build();
            assertThrows(SecurityFailure.class, () -> new HubVaultMetadataUVFProvider(JWEObjectJSON.parse(encrypted),
                    new JWKSet(fake)).getPayload());
        }
    }

    @Test
    void testDecrypt() throws Exception {
        // https://datatracker.ietf.org/doc/html/rfc7516#section-7.2.1
        final JWKSet jwks = JWKSet.parse("{\"keys\":[{\"kty\":\"oct\",\"kid\":\"org.cryptomator.hub.memberkey\",\"k\":\"dp5yWgrqhBNarwOEFDCXKv4h3lUf2pYKCCsXy-6TXDA\",\"alg\":\"A256KW\"}, {\"kty\":\"EC\",\"d\":\"8jz0iHA1jcp2dTB8NkVzTrDjpNhLeqfjLh-6iC2kWpc1VI-tMfK8T7gYjoNDJDDi\",\"crv\":\"P-384\",\"kid\":\"org.cryptomator.hub.recoverykey.2ZXISh-HhVaChehjaOZ6_n5Xl20fy6oAIkIrKkmMuNc\",\"x\":\"C1BXM5oQgz5AsfI7NblbYKMysWO72bQEbXbS7stgypKRxlOn4VQXQ2NkuFu0Ygom\",\"y\":\"7o8Wxe5efN-CXNsm8qe7prohViSl7a4TB8ilF5QHEh2vlIcJ5OXT2MZQyVXUZABo\",\"alg\":\"ECDH-ES+A256KW\"}]}");
        final String jwe = "{\"ciphertext\":\"mu1NLt7SrGeIvUp_LQ8MdX8NmrycbsSxkeU5k3eQ93AjXeWrd1yXLGTFknn7Ca37Oa5xUYZ2ghtROxnKPIMI00x_NZNgb29G9Ph8WMhj9xmASmVaJHB8qETT5PpB2InPS-E_cYE1KLuFjSVJh6XxtYaP0TSg-Qp0v-VJAu1zacksdHPzjBHJPYe-brumjV6treU5_6ODgWggvMV_C2fLUWWeDAZRQtl32GEmDQKwbpoxx__UcUeD4VkZs49PEbGXA1xzIraey3dnzCOLlO1N5nZYyEs0RLIxYWicR4zlvS-RJAmO6O0Y8MSC5jlzPChzcPnkF0j2Wf1VcVgKFeqXuwLSQ-09bkGikCHamg3GtGGuzBwZwQEVhEveOv8rB-iFGSLcljUqA0wDNRm8dEEjrED2XcsKyeVSKHBryzdeDDEjED_ZvSf9lQpBHcQoYLCrLKd0bg7lLU-tPgOlHHh2fRhP6_7lBg\",\"protected\":\"eyJ1dmYuc3BlYy52ZXJzaW9uIjoxLCJjdHkiOiJqc29uIiwiZW5jIjoiQTI1NkdDTSIsImNyaXQiOlsidXZmLnNwZWMudmVyc2lvbiJdLCJqa3UiOiJqd2tzLmpzb24iLCJjbG91ZC5rYXR0YS5vcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2dhdGV3YXkvYXBpL3ZhdWx0cy9iNjhiMDQ3My1lOTI0LTRlM2UtYWVhOS0zMTEzYmIzOWY1MDYvdXZmL3ZhdWx0LnV2ZiJ9\",\"recipients\":[{\"encrypted_key\":\"9tP7Dpr959jxCrfg5GrCxXiiQ24zPn1B_DayRq0_VqGpI7APVhudww\",\"header\":{\"alg\":\"A256KW\",\"kid\":\"org.cryptomator.hub.memberkey\"}},{\"encrypted_key\":\"YjxKzvDtcUV0LCuHFySPGlFIO8my65nn7g740lUp5vmA13jvsg18GQ\",\"header\":{\"epk\":{\"kty\":\"EC\",\"crv\":\"P-384\",\"x\":\"iXoJZC1IZ7gGK8dJ1SILmdVBxA-Z3L22tYtc8mnVD5iP6XhFfZCWZcjm7uomLQ8z\",\"y\":\"_5VK8uZnETmwsn3GGUV0A9AEft9_wGANPaeq6LxUJlGlZzeK4mTrw-Wo0lCG_Gaj\"},\"alg\":\"ECDH-ES+A256KW\",\"kid\":\"org.cryptomator.hub.recoverykey.2ZXISh-HhVaChehjaOZ6_n5Xl20fy6oAIkIrKkmMuNc\"}}],\"tag\":\"uP3byZ9RSicLD19rNfaAbQ\",\"iv\":\"MvlZJ8DvwR9tOWx6\"}";
        final String protectedDecode = new String(Base64.getDecoder().decode("eyJ1dmYuc3BlYy52ZXJzaW9uIjoxLCJjdHkiOiJqc29uIiwiZW5jIjoiQTI1NkdDTSIsImNyaXQiOlsidXZmLnNwZWMudmVyc2lvbiJdLCJqa3UiOiJqd2tzLmpzb24iLCJjbG91ZC5rYXR0YS5vcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2dhdGV3YXkvYXBpL3ZhdWx0cy9iNjhiMDQ3My1lOTI0LTRlM2UtYWVhOS0zMTEzYmIzOWY1MDYvdXZmL3ZhdWx0LnV2ZiJ9"), StandardCharsets.UTF_8);
        assertEquals("{\"uvf.spec.version\":1,\"cty\":\"json\",\"enc\":\"A256GCM\",\"crit\":[\"uvf.spec.version\"],\"jku\":\"jwks.json\",\"cloud.katta.origin\":\"https://example.com/gateway/api/vaults/b68b0473-e924-4e3e-aea9-3113bb39f506/uvf/vault.uvf\"}", protectedDecode);
        for(JWK key : jwks.getKeys()) {
            final UVFMetadataPayload meta = new HubVaultMetadataUVFProvider(JWEObjectJSON.parse(jwe), new JWKSet(key)).getPayload();
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
    void testMissingSpecVersion() throws Exception {
        final JWKSet jwks = HubVaultKeys.create().serialize();
        // header without additional critical param
        final JWEHeader header = new JWEHeader.Builder(EncryptionMethod.A256GCM)
                .jwkURL(URI.create("jwks.json"))
                .contentType("json")
                .build();
        final Payload payload = new Payload(new HashMap<String, Object>() {
        });
        final JWEObjectJSON builder = new JWEObjectJSON(header, payload);
        builder.encrypt(new MultiEncrypter(jwks));
        final SecurityFailure exc = assertThrows(SecurityFailure.class, () -> new HubVaultMetadataUVFProvider(builder, jwks.getKeyByKeyId("org.cryptomator.hub.memberkey")).getPayload());
        assertEquals("Missing value for critical header uvf.spec.version.", exc.getMessage());
    }

    @Test
    void testUvfVaultLoadFromMetadataPayload() throws Exception {
        final UVFMetadataPayload vaultMetadata = UVFMetadataPayload.create();
        final HubVaultKeys keys = HubVaultKeys.create();
        final UUID vaultId = UUID.randomUUID();
        final HubVaultMetadataUVFProvider provider = new HubVaultMetadataUVFProvider(
                vaultMetadata, "https://example.net/api", vaultId, keys.serialize());
        assertEquals(provider.computeRootDirIdHash(), provider.computeRootDirIdHash());
        final UVFVault vault = new UVFVault(Home.root());
        final Host host = new Host(new HubProtocol());
        vault.load(new HubSession(host, new DisabledX509TrustManager(), new DefaultX509KeyManager()), provider);
    }
}
