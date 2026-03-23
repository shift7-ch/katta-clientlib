/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.common.P384KeyPair;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;

/**
 * Represents a key set consisting of a member key and a recovery key for use within
 * cryptographic and key management operations. This class encapsulates the two keys
 * and provides methods for accessing them, as well as converting them into JWK (JSON Web Key)
 * representations.
 * <p>
 * The member key is commonly used for encryption and decryption, while the recovery key
 * provides redundancy and is primarily used for recovery scenarios.
 */
public final class UVFKeySet {
    private static final Logger log = LogManager.getLogger(UVFKeySet.class.getName());

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

    private final OctetSequenceKey memberKey;

    @Nullable
    private final P384KeyPair recoveryKey;

    public static UVFKeySet create() throws JOSEException {
        return new UVFKeySet();
    }

    /**
     * Constructs a new instance of the UniversalVaultFormatJWKS class.
     * This private constructor generates a default instance of the class, initializing memberKey and recoveryKey
     * using standard cryptographic algorithms and parameters.
     *
     * @throws JOSEException if there is an error during the creation of cryptographic keys.
     */
    private UVFKeySet() throws JOSEException {
        this(new OctetSequenceKeyGenerator(256)
                .keyID(KID_MEMBER_KEY)
                .algorithm(JWEAlgorithm.A256KW)
                .generate(), P384KeyPair.generate());
    }

    public UVFKeySet(final String memberKey) {
        this(new OctetSequenceKey.Builder(Base64.getDecoder().decode(memberKey))
                .keyID(KID_MEMBER_KEY)
                .algorithm(JWEAlgorithm.A256KW)
                .build(), null);
    }

    /**
     * Constructs a new instance of the UniversalVaultFormatJWKS class.
     * This constructor initializes the memberKey and recoveryKey properties, and generates a specific recoveryKeyJWK
     * for cryptographic operations using the provided memberKey and recoveryKey.
     *
     * @param memberKey   The OctetSequenceKey representing the member key (or vault key) used for encryption and decryption purposes.
     * @param recoveryKey The P384KeyPair containing the public and private components of the recovery key pair,
     *                    used for recovery scenarios and ensuring redundancy in cryptographic operations.
     */
    public UVFKeySet(final OctetSequenceKey memberKey, @Nullable final P384KeyPair recoveryKey) {
        this.memberKey = memberKey;
        this.recoveryKey = recoveryKey;
    }

    /**
     * Retrieves the recovery key associated with the vault. The recovery key is
     * represented by a {@link P384KeyPair}, which includes both the public and
     * private components of the key pair. This key is primarily used for
     * recovery scenarios and ensures cryptographic redundancy within the vault
     * system.
     *
     * @return the {@link P384KeyPair} containing the recovery key pair used for
     * recovery operations.
     */
    @Nullable
    public P384KeyPair recoveryKey() {
        return recoveryKey;
    }

    /**
     * Retrieves the member key
     *
     * @return the {@link OctetSequenceKey} representing the member key, which is used for encryption and decryption operations within the vault's cryptographic system.
     */
    public OctetSequenceKey memberKey() {
        return memberKey;
    }

    /**
     * Serializes the current key set into a {@link JWKSet}, which contains the cryptographic keys associated
     * with the instance. If a recovery key is available, it is included along with the member key; otherwise,
     * only the member key is serialized.
     *
     * @return A {@link JWKSet} containing the serialized cryptographic key set. The returned JWKSet may
     *         include both the member key and recovery key, if present.
     * @throws JOSEException If an error occurs during the serialization of cryptographic keys.
     */
    public JWKSet serialize() throws JOSEException {
        if(recoveryKey == null) {
            log.warn("Missing recovery key");
            return new JWKSet(memberKey);
        }
        return new JWKSet(Arrays.asList(memberKey, new ECKey.Builder(Curve.P_384, recoveryKey.getPublic())
                .algorithm(JWEAlgorithm.ECDH_ES_A256KW)
                .keyID(String.format(KID_RECOVERY_KEY_PREFIX, new ECKey.Builder(Curve.P_384, recoveryKey.getPublic()).build().computeThumbprint()))
                .privateKey(recoveryKey.getPrivate())
                .build()));
    }

    /**
     * Retrieve Recovery Key as JSON Web Key
     *
     * @return The JSON object string representation. Only public keys will be included
     */
    public static JWKSet serializePublicRecoveryKey(final P384KeyPair recoveryKey) throws JOSEException {
        return new JWKSet(new ECKey.Builder(Curve.P_384, recoveryKey.getPublic())
                .algorithm(JWEAlgorithm.ECDH_ES_A256KW)
                .keyID(String.format(KID_RECOVERY_KEY_PREFIX, new ECKey.Builder(Curve.P_384, recoveryKey.getPublic()).build().computeThumbprint()))
                .build()).toPublicJWKSet();
    }
}
