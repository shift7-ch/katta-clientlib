/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;

import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

import cloud.katta.crypto.UserKeys;
import com.amazonaws.util.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;

import static cloud.katta.crypto.KeyHelper.decodeKeyPair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UvfAccessTokenPayloadTest {
    // key pairs from frontend tests (crypto.spec.ts):
    private static final String USER_PRIV_KEY = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=";
    private static final String USER_PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW";
    private static final String DEVICE_PRIV_KEY = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDB2bmFCWy2p+EbAn8NWS5Om+GA7c5LHhRZb8g2pSMSf0fsd7k7dZDVrnyHFiLdd/YGhZANiAAR6bsjTEdXKWIuu1Bvj6Y8wySlIROy7YpmVZTY128ItovCD8pcR4PnFljvAIb2MshCdr1alX4g6cgDOqcTeREiObcSfucOU9Ry1pJ/GnX6KA0eSljrk6rxjSDos8aiZ6Mg=";
    private static final String DEVICE_PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEem7I0xHVyliLrtQb4+mPMMkpSETsu2KZlWU2NdvCLaLwg/KXEeD5xZY7wCG9jLIQna9WpV+IOnIAzqnE3kRIjm3En7nDlPUctaSfxp1+igNHkpY65Oq8Y0g6LPGomejI";


    final ECKey alice = new ECKey.Builder(
            Curve.P_384,
            new Base64URL("j6Retxx-L-rURQ4WNc8LvoqjbdPtGS6n9pCJgcm1U-NAWuWEvwJ_qi2tlrv_4w4p"),
            new Base64URL("wS-Emo-Q9qdtkHMJiDfVDAaxhF2-nSkDRn2Eg9CbG0pVwGEpaDybx_YYJwIaYooO")
    )
            .d(new Base64URL("wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_"))
            .build();


    @Test
    void testDecryptAccessToken() throws InvalidKeySpecException, ParseException, JOSEException, JsonProcessingException {
        final UserKeys userKeys = new UserKeys(decodeKeyPair(DEVICE_PUB_KEY, DEVICE_PRIV_KEY), decodeKeyPair(DEVICE_PUB_KEY, DEVICE_PRIV_KEY));
        final UvfAccessTokenPayload accessToken = userKeys.decryptAccessToken("eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp0cnVlLCJrdHkiOiJFQyIsIngiOiJoeHpiSWh6SUJza3A5ZkZFUmJSQ2RfOU1fbWYxNElqaDZhcnNoVXNkcEEyWno5ejZZNUs4NHpZR2I4b2FHemNUIiwieSI6ImJrMGRaNWhpelZ0TF9hN2hNejBjTUduNjhIRjZFdWlyNHdlclNkTFV5QWd2NWUzVzNYSG5sdHJ2VlRyU3pzUWYiLCJjcnYiOiJQLTM4NCJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..pu3Q1nR_yvgRAapG.4zW0xm0JPxbcvZ66R-Mn3k841lHelDQfaUvsZZAtWsL2w4FMi6H_uu6ArAWYLtNREa_zfcPuyuJsFferYPSNRUWt4OW6aWs-l_wfo7G1ceEVxztQXzQiwD30UTA8OOdPcUuFfEq2-d9217jezrcyO6m6FjyssEZIrnRArUPWKzGdghXccGkkf0LTZcGJoHeKal-RtyP8PfvEAWTjSOCpBlSdUJ-1JL3tyd97uVFNaVuH3i7vvcMoUP_bdr0XW3rvRgaeC6X4daPLUvR1hK5MsutQMtM2vpFghS_zZxIQRqz3B2ECxa9Bjxhmn8kLX5heZ8fq3lH-bmJp1DxzZ4V1RkWk.yVwXG9yARa5Ihq2koh2NbQ");
        assertEquals(USER_PRIV_KEY, accessToken.key());
    }

    @Test
    void decryptAccessToken() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        final ECKeyPair ecKeyPair = decodeKeyPair(Base64.encodeAsString(alice.toECPublicKey().getEncoded()), Base64.encodeAsString(alice.toECPrivateKey().getEncoded()));
        final UserKeys userKeys = new UserKeys(ecKeyPair, P384KeyPair.generate());
        final String accessToken = "eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoicFotVXExTjNOVElRcHNpZC11UGZMaW95bVVGVFJLM1dkTXVkLWxDcGh5MjQ4bUlJelpDc3RPRzZLTGloZnBkZyIsInkiOiJzMnl6eF9Ca2QweFhIcENnTlJFOWJiQUIyQkNNTF80cWZwcFEza1N2LXhqcEROVWZZdmlxQS1xRERCYnZkNDdYIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.I_rXJagNrrCa9zISf0DZJLQbIZDxEpGxCyjFbNE0iZs6yFeVayNOGQ.7rASe4SqyKJJLHZ4.l6T2N_ATytZUyh1IZTIJJDY4dXCyQVsRB19QIIPrAi0QQiS4gl4.fnOtAJhdvPFFHVi6L5Ma_R8iL3IXq1_xAq2PvdEfx0A";
        final UvfAccessTokenPayload accessTokenDecrypted = userKeys.decryptAccessToken(accessToken);
        assertEquals("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVU=", accessTokenDecrypted.key());
        assertNull(accessTokenDecrypted.recoveryKey);
    }

    @Test
    void encryptDecryptAccessToken() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        final ECKeyPair ecKeyPair = decodeKeyPair(USER_PUB_KEY, USER_PRIV_KEY);
        final UserKeys userKeys = new UserKeys(ecKeyPair, P384KeyPair.generate());
        final String accessToken = new UvfAccessTokenPayload().withKey("very secret").encryptForUser(userKeys.ecdhKeyPair().getPublic());
        final UvfAccessTokenPayload accessTokenDecrypted = userKeys.decryptAccessToken(accessToken);
        assertEquals("very secret", accessTokenDecrypted.key());
        assertNull(accessTokenDecrypted.recoveryKey);
    }
}
