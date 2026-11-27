/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.cryptomator.cryptolib.common.P384KeyPair;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.CRC32;

import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;

/**
 * Member key and recovery key for decrypting UVF metadata
 *
 * @see <a href="https://github.com/encryption-alliance/unified-vault-format">Unified Vault Format</a>
 */
public final class HubVaultKeys {
    private static final Logger log = LogManager.getLogger(HubVaultKeys.class.getName());

    /**
     * The value of this key, "org.cryptomator.hub.memberkey", is used as an identifier
     * to associate metadata or operations with the member key within the application
     * or external systems following the Universal Vault Format (UVF).
     */
    public static final String KID_MEMBER_KEY = "org.cryptomator.hub.memberkey";
    /**
     * A constant string prefix used to construct the Key ID (KID) for recovery keys
     * within the cryptographic system. The prefix is combined with a specific identifier
     * (formatted as a string) to generate a unique KID for each recovery key.
     * <p>
     * This prefix is primarily utilized in operations involving the recovery key,
     * such as creating or managing key identifiers in cryptographic contexts.
     * <p>
     * Format: "org.cryptomator.hub.recoverykey.%s"
     * <p>
     * The placeholder "%s" is intended to be replaced with a unique identifier
     * corresponding to a specific recovery key.
     */
    public static final String KID_RECOVERY_KEY_PREFIX = "org.cryptomator.hub.recoverykey.%s";

    /**
     * Member key shared with all vault members.
     */
    private final OctetSequenceKey memberKey;

    /**
     * Recovery key shared with all vault admins.
     */
    @Nullable
    private final P384KeyPair recoveryKey;

    /**
     * Initializing memberKey and recoveryKey using standard cryptographic algorithms and parameters.
     *
     * @return New key set
     * @throws SecurityFailure if there is an error during the creation of cryptographic keys.
     */
    public static HubVaultKeys create() throws SecurityFailure {
        try {
            return new HubVaultKeys();
        }
        catch(JOSEException e) {
            throw new SecurityFailure(e.getMessage(), e);
        }
    }

    /**
     * This private constructor generates a default instance of the class, initializing memberKey and recoveryKey
     * using standard cryptographic algorithms and parameters.
     *
     * @throws JOSEException if there is an error during the creation of cryptographic keys.
     */
    private HubVaultKeys() throws JOSEException {
        this(new OctetSequenceKeyGenerator(256)
                .keyID(KID_MEMBER_KEY)
                .algorithm(JWEAlgorithm.A256KW)
                .generate(), P384KeyPair.generate());
    }

    /**
     *
     * @param memberKey Base64 encoded member key
     */
    public HubVaultKeys(final String memberKey) {
        this(new OctetSequenceKey.Builder(Base64.getDecoder().decode(memberKey))
                .keyID(KID_MEMBER_KEY)
                .algorithm(JWEAlgorithm.A256KW)
                .build(), null);
    }

    /**
     *
     * @param memberKey   The OctetSequenceKey representing the member key (or vault key) used for encryption and decryption purposes.
     * @param recoveryKey The P384KeyPair containing the public and private components of the recovery key pair
     */
    public HubVaultKeys(final OctetSequenceKey memberKey, @Nullable final P384KeyPair recoveryKey) {
        this.memberKey = memberKey;
        this.recoveryKey = recoveryKey;
    }

    /**
     * Retrieves the recovery key associated with the vault. The recovery key is
     * represented by a {@link P384KeyPair}, which includes both the public and
     * private components of the key pair.
     *
     * @return The recovery key pair
     */
    @Nullable
    public P384KeyPair recoveryKey() {
        return recoveryKey;
    }

    /**
     * Retrieves the member key
     *
     * @return The member key
     */
    public OctetSequenceKey memberKey() {
        return memberKey;
    }

    /**
     * Serializes the private part of the recovery key in PKCS #8 format, appends a 16 bit checksum of the
     * least significant bytes of its CRC-32 and pads the result to a multiple of three bytes required for
     * the encoding as words.
     *
     * @return Private recovery key with checksum and padding
     */
    public static byte[] createRecoveryKey(final P384KeyPair recoveryKey) {
        // PKCS #8 encoded private key
        final byte[] rawkey = encodePrivateKey(recoveryKey);
        final CRC32 crc32 = new CRC32();
        crc32.update(rawkey, 0, rawkey.length);
        final long checksum = crc32.getValue();
        // Add 1-3 bytes of padding: 01 or 02 02 or 03 03 03
        final int padding = 3 - ((rawkey.length + 2) % 3);
        final byte[] combined = Arrays.copyOf(rawkey, rawkey.length + 2 + padding);
        // Append the least significant byte of the crc followed by the second-least significant byte
        combined[rawkey.length] = (byte) (checksum & 0xff);
        combined[rawkey.length + 1] = (byte) ((checksum >> 8) & 0xff);
        Arrays.fill(combined, rawkey.length + 2, combined.length, (byte) padding);
        return combined;
    }

    /**
     * Encodes the private key in PKCS #8 format with the same structure as the web frontend exporting with
     * WebCrypto, independent of the security provider. The curve is only identified in the algorithm identifier
     * and omitted in the ECPrivateKey structure (RFC 5915) which includes the public key. The default encoding
     * of the private key differs between providers (BouncyCastle includes the optional parameters), resulting
     * in a different recovery key for the same key pair.
     *
     * @param recoveryKey Recovery key pair
     * @return DER encoded PrivateKeyInfo
     */
    private static byte[] encodePrivateKey(final P384KeyPair recoveryKey) {
        final ECPrivateKey privateKey = (ECPrivateKey) recoveryKey.getPrivate();
        final ECPublicKey publicKey = (ECPublicKey) recoveryKey.getPublic();
        final ECNamedCurveParameterSpec curve = ECNamedCurveTable.getParameterSpec("secp384r1");
        // Uncompressed point 0x04 || x || y
        final byte[] publicPoint = curve.getCurve().createPoint(publicKey.getW().getAffineX(), publicKey.getW().getAffineY()).getEncoded(false);
        try {
            return new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, SECObjectIdentifiers.secp384r1),
                    new org.bouncycastle.asn1.sec.ECPrivateKey(curve.getN().bitLength(), privateKey.getS(), new DERBitString(publicPoint), null))
                    .getEncoded(ASN1Encoding.DER);
        }
        catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Restores the recovery key pair from its human-readable representation.
     *
     * @param recoveryKey Private recovery key encoded as a list of words
     * @return Recovery key pair including the public key derived from the private key
     * @throws SecurityFailure If the recovery key contains unknown words, has an invalid padding or checksum or is not a P-384 private key
     * @see #recoverRecoveryKey(byte[])
     */
    public static P384KeyPair recoverRecoveryKey(final String recoveryKey) throws SecurityFailure {
        final byte[] decoded;
        try {
            decoded = new WordEncoder().decode(recoveryKey);
        }
        catch(IllegalArgumentException e) {
            throw new SecurityFailure(e.getMessage(), e);
        }
        return recoverRecoveryKey(decoded);
    }

    /**
     * Restores the recovery key pair after verifying padding and checksum added in {@link #createRecoveryKey(P384KeyPair)}.
     *
     * @param recoveryKey Private recovery key with checksum and padding
     * @return Recovery key pair including the public key derived from the private key
     * @throws SecurityFailure If the padding or checksum is invalid or the data is not a P-384 private key in PKCS #8 format
     */
    public static P384KeyPair recoverRecoveryKey(final byte[] recoveryKey) throws SecurityFailure {
        if(recoveryKey.length < 3) {
            throw new SecurityFailure("Invalid recovery key length");
        }
        final int padding = recoveryKey[recoveryKey.length - 1];
        if(padding < 1 || padding > 3) {
            throw new SecurityFailure("Invalid padding");
        }
        for(int i = recoveryKey.length - padding; i < recoveryKey.length; i++) {
            if(recoveryKey[i] != padding) {
                throw new SecurityFailure("Invalid padding");
            }
        }
        final int length = recoveryKey.length - padding - 2;
        if(length <= 0) {
            throw new SecurityFailure("Invalid recovery key length");
        }
        final byte[] rawkey = Arrays.copyOf(recoveryKey, length);
        final CRC32 crc32 = new CRC32();
        crc32.update(rawkey, 0, rawkey.length);
        final long checksum = crc32.getValue();
        if(recoveryKey[length] != (byte) (checksum & 0xff)
                || recoveryKey[length + 1] != (byte) ((checksum >> 8) & 0xff)) {
            throw new SecurityFailure("Invalid recovery key checksum");
        }
        try {
            final KeyFactory factory = KeyFactory.getInstance("EC");
            final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(rawkey);
            final PrivateKey privateKey = factory.generatePrivate(privateKeySpec);
            if(!(privateKey instanceof ECPrivateKey)) {
                throw new SecurityFailure("Recovery key is not an EC private key");
            }
            final ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
            if(ecPrivateKey.getParams().getCurve().getField().getFieldSize() != 384) {
                throw new SecurityFailure("Recovery key is not a P-384 private key");
            }
            // Derive public point Q = d * G
            final ECNamedCurveParameterSpec curve = ECNamedCurveTable.getParameterSpec("secp384r1");
            final org.bouncycastle.math.ec.ECPoint q = new FixedPointCombMultiplier().multiply(curve.getG(), ecPrivateKey.getS()).normalize();
            final PublicKey publicKey = factory.generatePublic(new ECPublicKeySpec(
                    new ECPoint(q.getAffineXCoord().toBigInteger(), q.getAffineYCoord().toBigInteger()), ecPrivateKey.getParams()));
            return P384KeyPair.create(new X509EncodedKeySpec(publicKey.getEncoded()), privateKeySpec);
        }
        catch(NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SecurityFailure(e.getMessage(), e);
        }
    }

    /**
     * Serializes the current key set into a {@link JWKSet}.
     * with the instance. If a recovery key is available, it is included along with the member key; otherwise,
     * only the member key is serialized.
     *
     * @return A {@link JWKSet} containing the serialized cryptographic key set. The returned JWKSet may
     * include both the member key and recovery key, if present.
     * @throws SecurityFailure If an error occurs during the serialization of cryptographic keys.
     */
    public JWKSet serialize() throws SecurityFailure {
        if(recoveryKey == null) {
            log.warn("Missing recovery key");
            return new JWKSet(memberKey);
        }
        try {
            return new JWKSet(Arrays.asList(memberKey, new ECKey.Builder(Curve.P_384, recoveryKey.getPublic())
                    .algorithm(JWEAlgorithm.ECDH_ES_A256KW)
                    .keyID(String.format(KID_RECOVERY_KEY_PREFIX, new ECKey.Builder(Curve.P_384, recoveryKey.getPublic()).build().computeThumbprint()))
                    .privateKey(recoveryKey.getPrivate())
                    .build()));
        }
        catch(JOSEException e) {
            throw new SecurityFailure(e.getMessage(), e);
        }
    }
}
