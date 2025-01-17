/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;

import ch.iterate.hub.crypto.exceptions.InvalidSignatureException;
import ch.iterate.hub.crypto.exceptions.JWTParseException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;

/**
 * ES384 JWT (signed with ECDSA using P-384 and SHA-384):
 * <a href="https://www.rfc-editor.org/rfc/rfc7515">RFC 7515</a> / <a href="https://www.rfc-editor.org/rfc/rfc7519">RFC 7519</a>.
 * <p>
 * Counterpart of @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/jwt.ts">jwt.ts</a>
 */
public class JWT {
    /**
     * Decodes and verifies an ES384 JWT (signed with ECDSA using P-384 and SHA-384).
     *
     * @param jwt             The JWT to be decoded and verified.
     * @param signerPublicKey The signer public key to be verified.
     * @return header and payload
     * @throws Error if the JWT is invalid
     */
    public static Payload parse(final String jwt, final ECPublicKey signerPublicKey) throws ParseException, JOSEException, JWTParseException, InvalidSignatureException {
        final SignedJWT signedJWT = SignedJWT.parse(jwt);

        if(signedJWT.getHeader().getAlgorithm() != JWSAlgorithm.ES384) {
            throw new JWTParseException(String.format("Unsupported algorithm %s", signedJWT.getHeader().getAlgorithm()));
        }
        final boolean validSignature = es384verify(jwt, signerPublicKey);
        if(!validSignature) {
            throw new InvalidSignatureException("Invalid signature");
        }
        return signedJWT.getPayload();
    }

    /**
     * Creates an ES384 JWT (signed with ECDSA using P-384 and SHA-384).
     * <p>
     * See <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519</a>,
     * <a href="https://datatracker.ietf.org/doc/html/rfc7515">RFC 7515</a> and
     * <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.4">RFC 7518, Section 3.4</a>
     *
     * @param payload          The payload
     * @param signerPrivateKey The signers's private key
     */
    public static String build(final JWSHeader header, final Payload payload, final ECPrivateKey signerPrivateKey) throws ParseException, JOSEException {
        return es384sign(header, payload.toBase64URL(), signerPrivateKey);
    }

    // visible for testing
    public static String es384sign(final JWSHeader header, final Base64URL encodedPayload, final ECPrivateKey signerPrivateKey) throws JOSEException {
        final JWSObject jwsObject = new JWSObject(
                // ECDSA using P-384 curve and SHA-384 hash algorithm (optional).
                header,
                new Payload(encodedPayload)
        );

        jwsObject.sign(new ECDSASigner(signerPrivateKey));
        return jwsObject.serialize();
    }

    // visible for testing
    public static boolean es384verify(final String jwt, final ECPublicKey signerPublicKey) throws ParseException, JOSEException {
        final SignedJWT signedJWT = SignedJWT.parse(jwt);
        final Base64URL signature = signedJWT.getSignature();

        final ECDSAVerifier verifier = new ECDSAVerifier(signerPublicKey);
        final byte[] headerAndPayload = signedJWT.getSigningInput();
        return verifier.verify(signedJWT.getHeader(), headerAndPayload, signature);
    }
}
