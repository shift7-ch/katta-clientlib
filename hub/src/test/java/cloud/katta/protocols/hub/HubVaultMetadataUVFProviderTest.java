/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.cryptomator.impl.uvf.UVFVault;
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
        final HashMap<String, String> keys = new HashMap<String, String>() {{
            put("key01", Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            put("key02", Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        }};
        final UVFMetadataPayload orig = new UVFMetadataPayload()
                .withFileFormat("AES-256-GCM-32k")
                .withNameFormat("AES-256-SIV")
                .withSeeds(keys)
                .withLatestSeed("key01")
                .withinitialSeed("key02")
                .withKdf("1STEP-HMAC-SHA512")
                .withStorage(
                        new VaultMetadataStorageDto()
                                .provider("provider")
                                .nickname("nickname")
                                .bucket("bucket")
                ).withAutomaticAccessGrant(new VaultMetadataAutomaticAccessGrantDto()
                        .enabled(true)
                        .trustThreshold(42)
                );

        final HubVaultKeys jwks = HubVaultKeys.create();
        final OctetSequenceKey memberKey = jwks.memberKey();
        final Base64URL thumbprint = new ECKey.Builder(Curve.P_384, jwks.recoveryKey().getPublic()).build().computeThumbprint();
        final ECKey recoveryKey = ((ECKey) jwks.serialize().getKeyByKeyId(String.format(HubVaultKeys.KID_RECOVERY_KEY_PREFIX, thumbprint)));

        final UUID vaultId = UUID.randomUUID();

        final String encrypted = new HubVaultMetadataUVFProvider(orig, "https://example.com/gateway/api/", vaultId, jwks.serialize()).encrypt();

        // decrypt with memberKey
        {
            final UVFMetadataPayload decrypted = new HubVaultMetadataUVFProvider(JWEObjectJSON.parse(encrypted),
                    new JWKSet(memberKey)).getPayload();
            assertEquals(String.format("https://example.com/gateway/api/vaults/%s/uvf/vault.uvf", vaultId),
                    JWEObjectJSON.parse(encrypted).getHeader().getCustomParams().get("cloud.katta.origin"));
            assertEquals(orig, decrypted);
        }

        // decrypt with recoveryKey
        {
            final UVFMetadataPayload decrypted = new HubVaultMetadataUVFProvider(JWEObjectJSON.parse(encrypted),
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
        final JWKSet jwks = JWKSet.parse("{\"keys\":[{\"kty\":\"oct\",\"kid\":\"org.cryptomator.hub.memberkey\",\"k\":\"MVVPlljNLz9U5RFXL69Ayhio64QL-LtObty1G5kDxuQ\",\"alg\":\"A256KW\"}, {\"kty\":\"EC\",\"d\":\"NTfdwokq1q6qA8FZ-jdZ09LrBY4oI6UHL40_2bQ64HI0KIhGYIOwFGBwX2U50Eei\",\"crv\":\"P-384\",\"kid\":\"org.cryptomator.hub.recoverykey.k60X5s2Jie8fadgmy9HPnv4c_1kEv7qOVdP1j8vKjbA\",\"x\":\"Q_QS3CJbiLqczASmiKbYJtb1sf3nxoKtL6ooH4I-mI9XcygjhDwos6-XfRq_xFh1\",\"y\":\"AF9UnxL6FBolRVfrJw0ZNRBsFYPeqqc98rXdfn8je7HzysDWPQA_XNJ18YJtNPsi\",\"alg\":\"ECDH-ES+A256KW\"}]}\n");
        // protected: {"uvf.spec.version":1,"cty":"json","enc":"A256GCM","crit":["uvf.spec.version"],"jku":"jwks.json","cloud.katta.origin":"https://example.com/gateway/api/vaults/b68b0473-e924-4e3e-aea9-3113bb39f506/uvf/vault.uvf"}
        final String jwe = "{\"protected\":\"eyJjdHkiOiJqc29uIiwiY3JpdCI6WyJ1dmYuc3BlYy52ZXJzaW9uIl0sInV2Zi5zcGVjLnZlcnNpb24iOjEsImNsb3VkLmthdHRhLm9yaWdpbiI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYXBpLy92YXVsdHMvMTIzL3V2Zi92YXVsdC51dmYiLCJqa3UiOiJqd2tzLmpzb24iLCJlbmMiOiJBMjU2R0NNIn0\",\"recipients\":[{\"header\":{\"kid\":\"org.cryptomator.hub.memberkey\",\"alg\":\"A256KW\"},\"encrypted_key\":\"oMvz_GY-Lg4XJNvNVEUut4gnvwn-a9k16AysFKnryFp2Wy4lZqZ40g\"},{\"header\":{\"kid\":\"org.cryptomator.hub.recoverykey.k60X5s2Jie8fadgmy9HPnv4c_1kEv7qOVdP1j8vKjbA\",\"alg\":\"ECDH-ES+A256KW\",\"epk\":{\"key_ops\":[],\"ext\":true,\"kty\":\"EC\",\"x\":\"ZjquA9iBhxzIxM23m4cULBgClvJM7GMY-askSvTyLn8Hc8FRIEH8tkms3m19Odsk\",\"y\":\"MJuhBwrpKsG7_S_imlv6R_mDe-3-NBCwiGJk3WhXM3_1vQbH2hxLzo-MTjWf4AaI\",\"crv\":\"P-384\"},\"apu\":\"\",\"apv\":\"\"},\"encrypted_key\":\"gx_ccI0xlDMB7XLVRQlmDMLPlbH70XwimVkeqcWLwdtM_z4XCrwrpg\"}],\"iv\":\"ueBmTvYT_bcEE01a\",\"ciphertext\":\"XG8WBC0kCZDVFGy7t9ARetwDsMQuBcz-M3byQctQDBOszOwGmyYPVM2KOiEPayfe55QyJzHqSYolwPkvf4vS2o_D6JYJ4GA_VvdxIpdaOhl3zE-bAyW2fMpUKY6IFDjQDAs147tO2s_zCtr_q27idRKgdK0T10RwPY-tDmEPrStUS-KqXsim53XW3GNLWjPMUh5jgJQtGeXiGZ5gejC8rTxMBCYcTXORYe4yAf1kzHIuQTgPFQJA93NuQnO5t8Gohdj_YBGmmXwE58h4uIFkYSSayWt1dVEUKrM6xJFb6bshZmO3e0vn0svdpxuadcnA_LsCwh7-73s1m7dP4520O27G2_EaJMwIGhsWNx2mUC17Xco4tnb8xPE00m_oezU2DAEbhVjCmr6dwFLpAtDg9rQ6PFDWEfk\",\"tag\":\"JSGRv9Ygtne4oW1nOaHt0055iiDELleCNlEHm9B6Luk\"}";
        for(JWK key : jwks.getKeys()) {
            final UVFMetadataPayload meta = new HubVaultMetadataUVFProvider(JWEObjectJSON.parse(jwe), new JWKSet(key)).getPayload();
            assertEquals("AES-256-GCM-32k", meta.fileFormat());
            assertEquals("AES-SIV-512-B64URL", meta.nameFormat());
            assertEquals(1, meta.seeds().size());
            assertEquals("Ltg87d0_EnV8i5Rmnt4r48tMTlKc5q5mTcCkBcL1eXk", meta.seeds().get("WOpxyg"));
            assertEquals("WOpxyg", meta.initialSeed());
            assertEquals("WOpxyg", meta.latestSeed());
            assertEquals("HKDF-SHA512", meta.kdf());
            assertEquals("Sv9EiiT3ekfuVVWYJw26t28JyOQYi3JOFeQ8vRt-fzQ", meta.kdfSalt());
            assertEquals(true, meta.automaticAccessGrant().getEnabled());
            assertEquals(1, meta.automaticAccessGrant().getTrustThreshold());
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
