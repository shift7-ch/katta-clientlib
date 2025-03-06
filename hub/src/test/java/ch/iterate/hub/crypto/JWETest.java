/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto;

import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;

import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import ch.iterate.hub.model.JWEPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.Payload;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JWETest {
    @Test
    public void testEcdhEsEncryptDecrypt() throws ParseException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException, JsonProcessingException {
        final P384KeyPair keyPair = P384KeyPair.generate();
        final Map<String, Object> orig = Collections.singletonMap("hello", "world");
        final String jwe = JWE.ecdhEsEncrypt(new JWEPayload() {
            @Override
            public Map<String, Object> toJSONObject() {
                return orig;
            }
        }, "newkids", keyPair.getPublic());
        final Payload decrypted = JWE.decryptEcdhEs(jwe, keyPair.getPrivate());
        assertEquals(Collections.singletonMap("hello", "world"), decrypted.toJSONObject());
    }

    @Test
    public void testPbes2EncryptDecrypt() throws JOSEException, ParseException, JsonProcessingException {
        final String setupCode = "topsecret";
        final JWEPayload payload = new JWEPayload() {
            @Override
            public Map<String, Object> toJSONObject() {
                return Collections.singletonMap("hello", "world");
            }
        };
        final String jwe = JWE.pbes2Encrypt(payload, "kiddo", setupCode);
        final Payload decryptedPayload = JWE.decryptPbes2(jwe, setupCode);
        assertEquals(1, decryptedPayload.toJSONObject().size());
        assertEquals("world", decryptedPayload.toJSONObject().get("hello"));
    }

    @Test
    public void testDecryptPbes2() throws ParseException, JOSEException {
        final Payload payload = JWE.decryptPbes2("eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIiwicDJzIjoiQmp4NXBKU0lrSU5EZHJOS0pjTmdiUSIsInAyYyI6MTAwMDAwMCwiYXB1IjoiIiwiYXB2IjoiIn0.-sXeL9JD6MMhGEkdtP9LmIXGCHnVI5d7ifjO5WnWoz7d8Qsl7RmiQA.lmo9F4fSsukqfvuc.3o1l8RVc62czjv-fdi-jQJnijapO.n2ou6Oi04QOeGtiqrQhFRw", "topsecret");
        assertEquals(payload.toJSONObject().get("key"), "hello world");
    }
}
