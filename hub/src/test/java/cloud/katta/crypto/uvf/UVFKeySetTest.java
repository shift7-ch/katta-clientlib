/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UVFKeySetTest {

    @Test
    void serializePublicRecoveryKey() throws JOSEException {
        final UVFKeySet keys = UVFKeySet.create();
        assertTrue(keys.serialize().containsNonPublicKeys());
        assertFalse(UVFKeySet.serializePublicRecoveryKey(keys.recoveryKey()).containsNonPublicKeys());
    }
}
