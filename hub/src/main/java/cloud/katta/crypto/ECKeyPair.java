/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

/**
 * EC key pair with a public key validated to be a point on the curve. Replaces the class of the same name removed in
 * cryptolib 3.0.0.
 *
 * @see <a href="https://neilmadden.blog/2017/05/17/so-how-do-you-validate-nist-ecdh-public-keys/">Validating NIST ECDH public keys</a>
 */
public class ECKeyPair implements Destroyable {

    private final KeyPair keyPair;
    private boolean destroyed;

    /**
     * @param keyPair     EC key pair
     * @param curveParams Parameters of the expected curve
     * @throws IllegalArgumentException If the key pair is not an EC key pair or the public key is not a valid point
     *                                  on the curve
     */
    protected ECKeyPair(final KeyPair keyPair, final ECParameterSpec curveParams) {
        if(!(keyPair.getPrivate() instanceof ECPrivateKey) || !(keyPair.getPublic() instanceof ECPublicKey)) {
            throw new IllegalArgumentException("Not an EC key pair");
        }
        validate((ECPublicKey) keyPair.getPublic(), curveParams);
        this.keyPair = keyPair;
    }

    /**
     * Full public key validation for curves with cofactor 1 such as P-384
     */
    private static void validate(final ECPublicKey publicKey, final ECParameterSpec curveParams) {
        if(curveParams.getCofactor() != 1) {
            throw new IllegalArgumentException("Curves with cofactor other than 1 not supported");
        }
        final ECPoint point = publicKey.getW();
        if(ECPoint.POINT_INFINITY.equals(point)) {
            throw new IllegalArgumentException("Invalid EC public key at infinity");
        }
        final EllipticCurve curve = curveParams.getCurve();
        final BigInteger p = ((ECFieldFp) curve.getField()).getP();
        final BigInteger x = point.getAffineX();
        final BigInteger y = point.getAffineY();
        if(x.signum() < 0 || x.compareTo(p) >= 0 || y.signum() < 0 || y.compareTo(p) >= 0) {
            throw new IllegalArgumentException("Invalid EC public key with coordinates out of range");
        }
        // y^2 = x^3 + ax + b (mod p)
        final BigInteger left = y.multiply(y).mod(p);
        final BigInteger right = x.pow(3).add(curve.getA().multiply(x)).add(curve.getB()).mod(p);
        if(!left.equals(right)) {
            throw new IllegalArgumentException("Invalid EC public key not on curve");
        }
    }

    public KeyPair keyPair() {
        if(destroyed) {
            throw new IllegalStateException("Key pair destroyed");
        }
        return keyPair;
    }

    public ECPrivateKey getPrivate() {
        if(destroyed) {
            throw new IllegalStateException("Key pair destroyed");
        }
        return (ECPrivateKey) keyPair.getPrivate();
    }

    public ECPublicKey getPublic() {
        if(destroyed) {
            throw new IllegalStateException("Key pair destroyed");
        }
        return (ECPublicKey) keyPair.getPublic();
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        try {
            keyPair.getPrivate().destroy();
        }
        catch(DestroyFailedException e) {
            // Not supported by JDK key implementations
        }
        destroyed = true;
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        return MessageDigest.isEqual(keyPair.getPublic().getEncoded(), ((ECKeyPair) o).keyPair.getPublic().getEncoded());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keyPair.getPublic().getEncoded());
    }
}
