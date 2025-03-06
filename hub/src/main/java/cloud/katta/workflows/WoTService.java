/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import cloud.katta.client.ApiException;
import cloud.katta.client.model.TrustedUserDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.wot.SignedKeys;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

public interface WoTService {
    /**
     * Get list of verified trusted user IDs.
     *
     * @return IDs of trusted users with their trust level, skipping unverified trusts.
     */
    Map<String, Integer> getTrustLevelsPerUserId(UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;

    /**
     * Verifies a chain of signatures, where each signature signs the public key of the next signature.
     *
     * @param signatureChain   The signature chain, where the first element is signed by me
     * @param allegedSignedKey The public key that should be signed by the last signature in the chain
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts">wot.ts/verify()</a>
     */
    void verify(final UserKeys userKeys, List<String> signatureChain, SignedKeys allegedSignedKey) throws ApiException, AccessException, SecurityFailure;

    /**
     * Signs the public key of a user with my private key and sends the signature to the backend.
     *
     * @param user            The user whose keys to sign
     * @return The new trust object created during the signing process
     */
    TrustedUserDto sign(final UserKeys userKeys, UserDto user) throws ApiException, ParseException, JOSEException, AccessException, SecurityFailure;
}
