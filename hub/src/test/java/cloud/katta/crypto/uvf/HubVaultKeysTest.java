/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.nimbusds.jose.jwk.ECKey;

import static org.junit.jupiter.api.Assertions.*;

class HubVaultKeysTest {

    /**
     * Test vector from katta-server frontend/test/common/universalVaultFormat.spec.ts
     */
    private static final String PRIVATE_RECOVERY_KEY = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=";
    private static final String PUBLIC_RECOVERY_KEY = "{\"kid\":\"org.cryptomator.hub.recoverykey.T-LR82IaI1_TGHwcyn1u8vAYakGPz4upg1lPnE0xBZQ\",\"kty\":\"EC\",\"crv\":\"P-384\",\"x\":\"SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ\",\"y\":\"hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW\"}";
    private static final String RECOVERY_KEY = "cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig war linear tour sleep humanity threat question neglect stance radar bank coup misery painter tragedy buddy compare winter national approval budget deep screen outdoor audience tear stream cure type ugly chamber supporter franchise accept sexy ad imply being drug doctor regime where thick dam training grass chamber domestic dictator educate sigh music spoken connected measure voice lemon pig comprise disturb appear greatly satisfied heat news curiosity top impress nor method reflect lesson recommend dual revenge thorough bus count broadband living riot prejudice target blonde excess company thereby tribe respond horror mere way proud shopping wise liver mortgage plastic gentleman eighteen terms worry melt";

    @Test
    void serializePublicRecoveryKey() throws Exception {
        final HubVaultKeys keys = HubVaultKeys.create();
        assertTrue(keys.serialize().containsNonPublicKeys());
        assertFalse(keys.serialize().toPublicJWKSet().containsNonPublicKeys());
    }

    @Test
    void createRecoveryKey() throws Exception {
        assertEquals(RECOVERY_KEY, new WordEncoder().encodePadded(HubVaultKeys.createRecoveryKey(recoveryKey())));
    }

    private static P384KeyPair recoveryKey() throws Exception {
        return P384KeyPair.create(
                new X509EncodedKeySpec(ECKey.parse(PUBLIC_RECOVERY_KEY).toECPublicKey().getEncoded()),
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_RECOVERY_KEY)));
    }
}
