/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static org.junit.jupiter.api.Assertions.*;

class P384KeyPairTest {

    @Test
    void testCreate() throws Exception {
        final P384KeyPair generated = P384KeyPair.generate();
        final P384KeyPair created = P384KeyPair.create(new X509EncodedKeySpec(generated.getPublic().getEncoded()),
                new PKCS8EncodedKeySpec(generated.getPrivate().getEncoded()));
        assertEquals(generated, created);
        assertArrayEquals(generated.getPrivate().getEncoded(), created.getPrivate().getEncoded());
        assertNotEquals(generated, P384KeyPair.generate());
    }

    @Test
    void testRejectOtherCurve() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        final KeyPair other = generator.generateKeyPair();
        assertThrows(InvalidKeySpecException.class, () -> P384KeyPair.create(new X509EncodedKeySpec(other.getPublic().getEncoded()),
                new PKCS8EncodedKeySpec(other.getPrivate().getEncoded())));
    }

    @Test
    void testRejectPointNotOnCurve() throws Exception {
        final P384KeyPair valid = P384KeyPair.generate();
        final ECPoint point = valid.getPublic().getW();
        final ECPublicKey invalid = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(
                new ECPoint(point.getAffineX(), point.getAffineY().add(BigInteger.ONE)), valid.getPublic().getParams()));
        assertThrows(InvalidKeySpecException.class, () -> P384KeyPair.create(new X509EncodedKeySpec(invalid.getEncoded()),
                new PKCS8EncodedKeySpec(valid.getPrivate().getEncoded())));
    }

    @Test
    void testDestroy() {
        final P384KeyPair keyPair = P384KeyPair.generate();
        assertFalse(keyPair.isDestroyed());
        keyPair.destroy();
        assertTrue(keyPair.isDestroyed());
        assertThrows(IllegalStateException.class, keyPair::getPrivate);
        assertThrows(IllegalStateException.class, keyPair::getPublic);
        assertThrows(IllegalStateException.class, keyPair::keyPair);
    }
}
