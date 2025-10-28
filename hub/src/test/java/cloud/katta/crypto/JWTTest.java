/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.cryptomator.cryptolib.common.ECKeyPair;
import org.junit.jupiter.api.Test;

import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

import cloud.katta.crypto.exceptions.InvalidSignatureException;
import cloud.katta.crypto.exceptions.JWTParseException;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;

import static cloud.katta.crypto.KeyHelper.decodeKeyPair;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <a href="https://www.rfc-editor.org/rfc/rfc7515">RFC 7515</a> / <a href="https://www.rfc-editor.org/rfc/rfc7519">RFC 7519</a>.
 *
 * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/test/common/jwt.spec.ts">jwt.spec.ts</a>
 */
class JWTTest {

    final ECKey signerPublicJwk = new ECKey.Builder(
            Curve.P_384,
            // key coordinates from MDN examples:
            new Base64URL("SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ"),
            new Base64URL("hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW"))
            .d(
                    // key coordinates from MDN examples:
                    new Base64URL("wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_")
            )
            .build();

    final ECKeyPair signerKey = decodeKeyPair(Base64.getEncoder().encodeToString(signerPublicJwk.toECPublicKey().getEncoded()),
            Base64.getEncoder().encodeToString(signerPublicJwk.toECPrivateKey().getEncoded()));

    JWTTest() throws InvalidKeySpecException, JOSEException {
    }

    @Test
    void es384signVerify() throws JOSEException, ParseException {
        final String encodedHeader = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0";
        final String encodedPayload = "eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0";
        final String jwt = JWT.es384sign(JWSHeader.parse(Base64URL.from(encodedHeader)), Base64URL.from(encodedPayload), signerKey.getPrivate());
        assertNotNull(jwt);
        assertTrue(JWT.es384verify(jwt, signerKey.getPublic()));
    }

    @Test
    void es384verifyValidSignature() throws ParseException, JOSEException {
        final String jwt = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0.eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0.9jS7HDRkbwEbmJ_cpkFHcQuNHSsOzSO3ObkT_FBQIIJehYYk-1aAK0KVnOgeDg6hVELy5-XcRHOCETwuTuYG5eQ3jIbxpTviHttJ-r26BYynw6dlmJTuLSvsTjtpoTa_";
        assertTrue(JWT.es384verify(jwt, signerKey.getPublic()));
    }

    @Test
    void es384verifyInvalidSignature() throws ParseException, JOSEException {
        final String jwt = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0.eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0.9jS7HDRkbwEbmJ_cpkFHcQuNHSsOzSO3ObkT_FBQIIJehYYk-1aAK0KVnOgeDg6hVELy5-XcRHOCETwuTuYG5eQ3jIbxpTviHttJ-r26BYynw6dlmJTuLSvsTjtpoTaX";
        assertFalse(JWT.es384verify(jwt, signerKey.getPublic()));
    }

    @Test
    void buildES384SignedJWTAndParse() throws ParseException, JOSEException, JWTParseException, InvalidSignatureException {
        final Map<String, Object> payloadExpected = ImmutableMap.<String, Object>builder()
                .put("foo", 42L)
                .put("bar", "lol")
                .put("obj", ImmutableMap.<String, Object>builder().put("nested", true).build())
                .build();
        final String jwt = JWT.build(new JWSHeader.Builder(JWSAlgorithm.ES384).type(JOSEObjectType.JWT).base64URLEncodePayload(true).build(), new Payload(payloadExpected), signerKey.getPrivate());
        assertNotNull(jwt);
        final Map<String, Object> payload = JWT.parse(jwt, signerKey.getPublic()).toJSONObject();
        assertEquals(42L, payload.get("foo"));
        assertEquals("lol", payload.get("bar"));
        final Map<String, Boolean> obj = (Map<String, Boolean>) payload.get("obj");
        assertEquals(1, obj.size());
        assertTrue(obj.get("nested"));
        assertEquals(3, payload.size());
    }

    @Test
    void parseES384SignedJWT() throws ParseException, JOSEException, JWTParseException, InvalidSignatureException {
        final String jwt = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0.eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0.9jS7HDRkbwEbmJ_cpkFHcQuNHSsOzSO3ObkT_FBQIIJehYYk-1aAK0KVnOgeDg6hVELy5-XcRHOCETwuTuYG5eQ3jIbxpTviHttJ-r26BYynw6dlmJTuLSvsTjtpoTa_";
        final Map<String, Object> payload = JWT.parse(jwt, signerKey.getPublic()).toJSONObject();
        assertEquals(42L, payload.get("foo"));
        assertEquals("lol", payload.get("bar"));
        final Map<String, Boolean> obj = (Map<String, Boolean>) payload.get("obj");
        assertEquals(1, obj.size());
        assertTrue(obj.get("nested"));
        assertEquals(3, payload.size());
    }
}
