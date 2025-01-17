/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto;

import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.MessageDigestSupplier;
import org.cryptomator.cryptolib.common.P384KeyPair;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.function.Supplier;

import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import com.google.common.io.BaseEncoding;

public class KeyHelper {

    // adapted from org.cryptomator.ui.keyloading.hub.HubKeyLoadingModule
    public static String getDeviceIdFromDeviceKeyPair(final ECKeyPair deviceKeyPair) {
        final Supplier<MessageDigest> instance = MessageDigestSupplier.SHA256.instance();
        final byte[] hashedKey = instance.get().digest(deviceKeyPair.getPublic().getEncoded());
        return BaseEncoding.base16().encode(hashedKey);
    }

    /**
     * @param publicKey PEM-encoded public key in Base64.
     */
    // adapted from org.cryptomator.hub.filters.VaultAdminOnlyFilterProvider
    public static ECPublicKey decodePublicKey(final String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        final EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));

        final KeyFactory keyFactory = KeyFactory.getInstance("EC");
        final PublicKey key = keyFactory.generatePublic(keySpec);

        if(key instanceof ECPublicKey) {
            return (ECPublicKey) key;
        }
        else {
            throw new NotECKeyException("Key not an EC public key.");
        }
    }

    /**
     * @param privateKey PKCS8-encoded in Base64
     */
    public static String encodePrivateKey(final ECPrivateKey privateKey) {
        final EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        return Base64.getEncoder().encodeToString(keySpec.getEncoded());
    }

    /**
     * @param publicKey PEM-encoded in Base64
     */
    public static String encodePublicKey(final ECPublicKey publicKey) {
        final EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        return Base64.getEncoder().encodeToString(keySpec.getEncoded());
    }

    /**
     * @param privateKey PKCS8-encoded in Base64
     */
    public static ECPrivateKey decodePrivateKey(final String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        final EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));

        final KeyFactory keyFactory = KeyFactory.getInstance("EC");
        final PrivateKey key = keyFactory.generatePrivate(privateKeySpec);

        if(key instanceof ECPrivateKey) {
            return (ECPrivateKey) key;
        }
        else {
            throw new NotECKeyException("Key not an EC private key.");
        }
    }

    /**
     * @param publicKey  PEM-encoded public key in Base64.
     * @param privateKey PKCS8-encoded in Base64
     */
    public static ECKeyPair decodeKeyPair(final String publicKey, final String privateKey) throws InvalidKeySpecException {
        final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
        final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        // TODO beware - may use BC!
        return P384KeyPair.create(publicKeySpec, privateKeySpec);
    }
}
