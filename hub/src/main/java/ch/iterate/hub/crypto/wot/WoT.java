/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.wot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.JWT;
import ch.iterate.hub.crypto.exceptions.InvalidSignatureException;
import ch.iterate.hub.crypto.exceptions.JWTParseException;
import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;

import static ch.iterate.hub.crypto.KeyHelper.decodePublicKey;

/**
 * Web of Trust signature chains.
 * Counterpart of @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts">wot.ts</a>
 */
public class WoT {
    private static final Logger log = LogManager.getLogger(WoT.class.getName());

    /**
     * Recursively verifies a chain of signatures, where each signature signs the public key of the next signature.
     *
     * @param signatureChain   The chain of signatures to verify
     * @param signerPublicKey  A trusted public key to verify the first signature in the chain
     * @param allegedSignedKey The public key that should be signed by the last signature in the chain
     * @throws SecurityException Error if the signature chain is invalid
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts">wot.ts/verifyRescursive()</a>
     */
    public static void verifyRecursive(final List<String> signatureChain, final ECPublicKey signerPublicKey, final SignedKeys allegedSignedKey) throws SecurityFailure {
        // get first element of signature chain
        final String signature = signatureChain.get(0);
        final SignedKeys signedKeys;
        try {
            signedKeys = SignedKeys.fromPayload(JWT.parse(signature, signerPublicKey));
        }
        catch(ParseException | InvalidSignatureException | JWTParseException | JOSEException e) {
            throw new SecurityFailure(e);
        }
        final List<String> remainingChain = signatureChain.subList(1, signatureChain.size());
        if(remainingChain.isEmpty()) {
            // last element in chain should match signed public key
            if(!allegedSignedKey.equals(signedKeys)) {
                throw new SecurityFailure(String.format("Alleged public key does not match signed public key. Expected: %s. Found %s.", allegedSignedKey, signedKeys));
            }
        }
        else {
            // otherwise, the payload is an intermediate public key used to sign the next element
            final ECPublicKey nextTrustedPublicKey;
            try {
                nextTrustedPublicKey = decodePublicKey(signedKeys.ecdsaPublicKey());
            }
            catch(NoSuchAlgorithmException | InvalidKeySpecException | NotECKeyException e) {
                throw new SecurityFailure(e);
            }
            verifyRecursive(remainingChain, nextTrustedPublicKey, allegedSignedKey);
        }
    }

    /**
     * Signs the public key of a user with my private key and returns the signature.
     *
     * @param signerPrivateKey My user keys
     * @param iss              The issuer (my user ID)
     * @param user             The user whose keys to sign
     * @return The signed key.
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts">wot.ts/sign()</a>
     */
    public static String sign(final ECPrivateKey signerPrivateKey, final String iss, final UserDto user) throws ParseException, JOSEException {
        return JWT.build(
                new JWSHeader.Builder(JWSAlgorithm.ES384)
                        .type(JOSEObjectType.JWT)
                        .base64URLEncodePayload(true)
                        .customParam("iss", iss)
                        .customParam("sub", user.getId())
                        .customParam("iat", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))
                        .build(),
                new SignedKeys(user.getEcdhPublicKey(), user.getEcdsaPublicKey()).toPayload(),
                signerPrivateKey
        );
    }

    /**
     * Length of signature chain after verification. -1 if unverified.
     *
     * @param trustedUser user for which trust level to be computed.
     * @param trust       trust for the user
     * @return Trust level. -1 for unbounded.
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/components/TrustDetails.vue">TrustDetails.vue</a>
     */
    public static int computeTrustLevel(final UserDto trustedUser, final TrustedUserDto trust, final UserDto signer) {
        final String myId = signer.getId();
        if(trust.getTrustedUserId() == null) {
            return -1; // unverified
        }
        if(myId == null) {
            return -1; // unverified
        }
        if(trust.getTrustedUserId().equals(myId)) {
            return 0;
        }
        if(trustedUser.getEcdhPublicKey() == null) {
            return -1; // unverified
        }
        if(trustedUser.getEcdsaPublicKey() == null) {
            return -1; // unverified
        }
        final List<String> signatureChain = trust.getSignatureChain();
        if(signatureChain == null) {
            return -1; // unverified
        }
        try {
            verifyRecursive(signatureChain, decodePublicKey(signer.getEcdsaPublicKey()), SignedKeys.fromUser(trustedUser));
            return signatureChain.size();
        }
        catch(SecurityFailure | NoSuchAlgorithmException | InvalidKeySpecException | NotECKeyException e) {
            if(log.isWarnEnabled()) {
                log.warn("WoT signature verification failed.", e);
            }
            return -1; // unverified
        }
    }

    /**
     * Verify all returned signature chains:
     * For each signature chain, the first element of each chain is signed by the current user,
     * i.e. the signature can be verified using their public key.
     * Each further element is signed by the respective user. The last element must be the key to be verified.
     *
     * @param trusts          signer's signature chains
     * @param users           other users
     * @param signerPublicKey signer's public key
     * @return the verified trusts
     */
    public static Map<TrustedUserDto, Integer> verifyTrusts(final List<TrustedUserDto> trusts, final List<UserDto> users, final ECPublicKey signerPublicKey) {
        final Map<TrustedUserDto, Integer> verified = new HashMap<>();
        for(final TrustedUserDto trust : trusts) {
            final String trustedUserId = trust.getTrustedUserId();
            if(trustedUserId == null) {
                if(log.isWarnEnabled()) {
                    log.warn("Verification for {} failed. No ID found for trustee.", trust);
                }
                continue;
            }
            final UserDto user = users.stream().filter(u -> trustedUserId.equals(u.getId())).findFirst().orElse(null);
            final List<String> signatureChain = trust.getSignatureChain();
            if(user == null || signatureChain == null) {
                if(log.isWarnEnabled()) {
                    log.warn("Verification for {} failed. No user or no signature chain found.", trust);
                }
                continue;
            }
            try {
                WoT.verifyRecursive(signatureChain, signerPublicKey, SignedKeys.fromUser(user));
                verified.put(trust, signatureChain.size());
            }
            catch(SecurityFailure e) {
                if(log.isWarnEnabled()) {
                    log.warn(String.format("Verification for %s failed - not granting access.", trust), e);
                }
            }
        }
        return verified;
    }
}
