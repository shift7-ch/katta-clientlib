/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;

import ch.iterate.hub.model.JWEPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.AESDecrypter;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.PasswordBasedDecrypter;
import com.nimbusds.jose.crypto.PasswordBasedEncrypter;
import com.nimbusds.jose.util.Base64URL;

/**
 * JWE using alg: ECDH-ES and enc: A256GCM.
 *
 * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts</a>
 */
public class JWE {

    private static final JWEAlgorithm ECDHES_DESIGNATION_ALG = JWEAlgorithm.ECDH_ES;
    private static final EncryptionMethod ECDHES_DESIGNATION_ENC = EncryptionMethod.A256GCM;

    private static final JWEAlgorithm PBES2_DESIGNATION_ALG = JWEAlgorithm.PBES2_HS512_A256KW;
    private static final EncryptionMethod PBES2_DESIGNATION_ENC = EncryptionMethod.A256GCM;

    private static final JWEAlgorithm A256KW_DESIGNATION_ALG = JWEAlgorithm.A256KW;
    private static final EncryptionMethod A256KW_DESIGNATION_ENC = EncryptionMethod.A256GCM;
    private static final int PBES2_ITERATION_COUNT = 1000000;
    private static final int PBES2_SALT_LENGTH = 16;

    /**
     * Prepares a new JWE using alg: ECDH-ES and enc: A256GCM.
     *
     * @param payload            The cek
     * @param recipientPublicKey recipient's public key
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/EcdhRecipient(password).encrypt(cek)</a>
     */
    public static String ecdhEsEncrypt(final JWEPayload payload, final String kid, final ECPublicKey recipientPublicKey) throws JOSEException, JsonProcessingException {
        return ecdhEsEncrypt(payload, kid, recipientPublicKey, "", "");
    }

    /**
     * Prepares a new JWE using alg: ECDH-ES and enc: A256GCM.
     *
     * @param payload            The cek
     * @param recipientPublicKey The recipient's public key
     * @param apu                Optional information about the creator
     * @param apv                Optional information about the recipient
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/EcdhRecipient(password).encrypt(cek)</a>
     */
    public static String ecdhEsEncrypt(final JWEPayload payload, final String kid, final ECPublicKey recipientPublicKey, final String apu, final String apv) throws JOSEException, JsonProcessingException {
        final JWEEncrypter jweEncrypter = new ECDHEncrypter(recipientPublicKey);
        final JWEHeader header = new JWEHeader.Builder(ECDHES_DESIGNATION_ALG, ECDHES_DESIGNATION_ENC)
                .keyID(kid)
                .agreementPartyUInfo(Base64URL.encode(apu))
                .agreementPartyVInfo(Base64URL.encode(apv))
                .build();

        final JWEObject jwe = new JWEObject(header, new Payload(payload.toJSONObject()));
        jwe.encrypt(jweEncrypter);
        return jwe.serialize();
    }

    /**
     * Creates a JWE using alg: PBES2-HS512+A256KW and enc: A256GCM.
     *
     * @param payload  The cek
     * @param password The password to feed into the KDF
     * @return JWE
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/Pbes2Recipient(password).encrypt(cek)</a>
     */
    public static String pbes2Encrypt(final JWEPayload payload, final String kid, final String password) throws JOSEException, JsonProcessingException {
        return pbes2Encrypt(payload, kid, password, "", "");
    }

    /**
     * Creates a JWE using alg: PBES2-HS512+A256KW and enc: A256GCM.
     *
     * @param payload  the payload to be encrypted
     * @param password The password to feed into the KDF
     * @param apu      Optional information about the creator
     * @param apv      Optional information about the recipient
     * @return JWE
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/Pbes2Recipient(password).encrypt(cek)</a>
     */
    public static String pbes2Encrypt(final JWEPayload payload, final String kid, final String password, final String apu, final String apv) throws JsonProcessingException, JOSEException {
        final JWEEncrypter jweEncrypter = new PasswordBasedEncrypter(password, PBES2_SALT_LENGTH, PBES2_ITERATION_COUNT);
        final JWEHeader header = new JWEHeader.Builder(PBES2_DESIGNATION_ALG, PBES2_DESIGNATION_ENC)
                .keyID(kid)
                .agreementPartyUInfo(Base64URL.encode(apu))
                .agreementPartyVInfo(Base64URL.encode(apv))
                .build();

        final JWEObject jwe = new JWEObject(header, new Payload(payload.toJSONObject()));
        jwe.encrypt(jweEncrypter);
        return jwe.serialize();
    }

    /**
     * Prepares a new JWE using alg: A256KW and enc: A256GCM.
     *
     * @param payload The cek
     * @param kek     The key used to wrap the CEK
     * @return JWE
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/A256kwRecipient(wrappingKey).encrypt(cek)</a>
     */
    public static String a256kwEncrypt(final JWEPayload payload, final String kid, final byte[] kek) throws JOSEException, JsonProcessingException {
        return a256kwEncrypt(payload, kid, kek, "", "");
    }

    /**
     * Prepares a new JWE using alg: A256KW and enc: A256GCM.
     *
     * @param payload The cek
     * @param kek     The key used to wrap the CEK
     * @param apu     Optional information about the creator
     * @param apv     Optional information about the recipient
     * @return JWE
     */
    public static String a256kwEncrypt(final JWEPayload payload, final String kid, final byte[] kek, final String apu, final String apv) throws JsonProcessingException, JOSEException {
        final JWEEncrypter jweEncrypter = new AESEncrypter(kek);
        final JWEHeader header = new JWEHeader.Builder(A256KW_DESIGNATION_ALG, A256KW_DESIGNATION_ENC)
                .keyID(kid)
                .agreementPartyUInfo(Base64URL.encode(apu))
                .agreementPartyVInfo(Base64URL.encode(apv))
                .build();
        final JWEObject jwe = new JWEObject(header, new Payload(payload.toJSONObject()));
        jwe.encrypt(jweEncrypter);
        return jwe.serialize();
    }

    /**
     * Decrypts the JWE, assuming alg == PBES2-HS512+A256KW and enc == A256GCM.
     *
     * @param password The password to feed into the KDF
     * @return The cek
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/EncryptedJWE.decrypt(Pbes2Recipient(password))</a>
     */
    public static Payload decryptPbes2(final String jwe, final String password) throws ParseException, JOSEException {
        final JWEDecrypter jweDecrypter = new PasswordBasedDecrypter(password);
        final JWEObject jweObject = JWEObject.parse(jwe);
        jweObject.decrypt(jweDecrypter);
        return jweObject.getPayload();
    }

    /**
     * Decrypts the JWE, assuming alg == ECDH-ES, enc == A256GCM and keys on the P-384 curve.
     *
     * @param jwe                 The JWE holding the encrypted private keys
     * @param recipientPrivateKey The recipient's private key
     * @return The cek
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/EncryptedJWE.decrypt(EcdhRecipient(recipientKey))</a>
     */
    public static Payload decryptEcdhEs(final String jwe, final ECPrivateKey recipientPrivateKey) throws ParseException, JOSEException {
        final JWEDecrypter jweDecrypter = new ECDHDecrypter(recipientPrivateKey);
        final JWEObject jweObject = JWEObject.parse(jwe);
        jweObject.decrypt(jweDecrypter);
        return jweObject.getPayload();
    }

    /**
     * Decrypts the JWE, assuming alg == A256KW and enc == A256GCM.
     *
     * @param jwe The JWE holding the encrypted private keys
     * @param kek The key used to wrap the CEK
     * @return The cek.
     * @throws ParseException,JOSEException if decryption failed (wrong kek?)
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwe.ts">jwe.ts/EncryptedJWE.decrypt(A256kwRecipient(wrappingKey))</a>
     */
    public static Payload decryptA256kw(final String jwe, final byte[] kek) throws ParseException, JOSEException {
        // TODO why do we not need to pass in kid????
        final JWEDecrypter jweDecrypter = new AESDecrypter(kek);
        final JWEObject jweObject = JWEObject.parse(jwe);
        jweObject.decrypt(jweDecrypter);
        return jweObject.getPayload();
    }
}
