/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubVaultKeysTest {

    @Test
    void serializePublicRecoveryKey() throws Exception {
        final HubVaultKeys keys = HubVaultKeys.create();
        assertTrue(keys.serialize().containsNonPublicKeys());
        assertFalse(keys.serialize().toPublicJWKSet().containsNonPublicKeys());
    }
}
