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
     * @throws SecurityFailure if there is an error during the creation of cryptographic keys.
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
