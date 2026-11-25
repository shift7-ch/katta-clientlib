/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Key pair on curve P-384 as used for device keys, user keys and vault recovery keys of Cryptomator Hub. Replaces the
 * class of the same name removed in cryptolib 3.0.0.
 */
public class P384KeyPair extends ECKeyPair {

    private static final String EC_ALG = "EC";
    private static final String EC_CURVE_NAME = "secp384r1";

    private P384KeyPair(final KeyPair keyPair) {
        super(keyPair, curveParams());
    }

    public static P384KeyPair generate() {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_ALG);
            generator.initialize(new ECGenParameterSpec(EC_CURVE_NAME));
            return new P384KeyPair(generator.generateKeyPair());
        }
        catch(NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(String.format("%s with curve %s not supported", EC_ALG, EC_CURVE_NAME), e);
        }
    }

    /**
     * @param publicKeySpec  DER encoded public key
     * @param privateKeySpec DER encoded private key
     * @return Key pair with validated public key
     * @throws InvalidKeySpecException If the keys are not a valid P-384 key pair
     */
    public static P384KeyPair create(final X509EncodedKeySpec publicKeySpec, final PKCS8EncodedKeySpec privateKeySpec) throws InvalidKeySpecException {
        try {
            final KeyFactory factory = KeyFactory.getInstance(EC_ALG);
            return new P384KeyPair(new KeyPair(factory.generatePublic(publicKeySpec), factory.generatePrivate(privateKeySpec)));
        }
        catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("%s not supported", EC_ALG), e);
        }
        catch(IllegalArgumentException e) {
            throw new InvalidKeySpecException(e.getMessage(), e);
        }
    }

    private static ECParameterSpec curveParams() {
        try {
            final AlgorithmParameters parameters = AlgorithmParameters.getInstance(EC_ALG);
            parameters.init(new ECGenParameterSpec(EC_CURVE_NAME));
            return parameters.getParameterSpec(ECParameterSpec.class);
        }
        catch(NoSuchAlgorithmException | InvalidParameterSpecException e) {
            throw new IllegalStateException(String.format("%s with curve %s not supported", EC_ALG, EC_CURVE_NAME), e);
        }
    }
}
