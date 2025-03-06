/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import cloud.katta.crypto.exceptions.NotECKeyException;

import static cloud.katta.crypto.KeyHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class KeyHelperTest {


    @Test
    public void decodePrivateEncodePrivate() throws NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        // N.B. BC returns encoding with additional information, SunEC returns same encoding, see KeyHelperTest#testElliptic
        // final String PRIV_KEY = "ME8CAQAwEAYHKoZIzj0CAQYFK4EEACIEODA2AgEBBDEA6QybmBitf94veD5aCLr7nlkF5EZpaXHCfq1AXm57AKQyGOjTDAF9EQB28fMywTDQ";
        final String PRIV_KEY = "MFcCAQAwEAYHKoZIzj0CAQYFK4EEACIEQDA+AgEBBDDpDJuYGK1/3i94PloIuvueWQXkRmlpccJ+rUBebnsApDIY6NMMAX0RAHbx8zLBMNCgBwYFK4EEACI=";
        assertEquals(PRIV_KEY, encodePrivateKey(decodePrivateKey(PRIV_KEY)));
    }

    @Test
    public void decodePublicEncodePublic() throws NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
        final String PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAERxQR+NRN6Wga01370uBBzr2NHDbKIC56tPUEq2HX64RhITGhii8Zzbkb1HnRmdF0aq6uqmUy4jUhuxnKxsv59A6JeK7Unn+mpmm3pQAygjoGc9wrvoH4HWJSQYUlsXDu";
        assertEquals(PUB_KEY, encodePublicKey(decodePublicKey(PUB_KEY)));
    }

    @Test
    @Disabled("for documentation, run isolated only")
    public void testElliptic() throws InvalidKeySpecException, NoSuchAlgorithmException, NotECKeyException {
        final String encodedPublicKey = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESA0oPUI0DrULBpIjTRckRduqaPqKz2f4zF6UvB+WHyOVZNsWZHHWIdjZ4LkoOygNenLgllv/iyzzVrH2ILR3Si6s03UOnicBbLy8jPY3MRvdgJPNz4C0kFa7HNXtQNKE";
        final String encodedPrivateKey = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCwqDLejJNvg83KP+zeNuLcrHnGjABYZXDq4NNxwHPtwv12vHDghCng4vM+qmN+EzWhZANiAARIDSg9QjQOtQsGkiNNFyRF26po+orPZ/jMXpS8H5YfI5Vk2xZkcdYh2NnguSg7KA16cuCWW/+LLPNWsfYgtHdKLqzTdQ6eJwFsvLyM9jcxG92Ak83PgLSQVrsc1e1A0oQ=";
        final String encodedPrivateKeyBC = "MIG/AgEAMBAGByqGSM49AgEGBSuBBAAiBIGnMIGkAgEBBDCwqDLejJNvg83KP+zeNuLcrHnGjABYZXDq4NNxwHPtwv12vHDghCng4vM+qmN+EzWgBwYFK4EEACKhZANiAARIDSg9QjQOtQsGkiNNFyRF26po+orPZ/jMXpS8H5YfI5Vk2xZkcdYh2NnguSg7KA16cuCWW/+LLPNWsfYgtHdKLqzTdQ6eJwFsvLyM9jcxG92Ak83PgLSQVrsc1e1A0oQ=";
        assertEquals("SunEC", KeyFactory.getInstance("EC").getProvider().getName());
        assertEquals(encodedPrivateKey, Base64.getEncoder().encodeToString(decodePrivateKey(encodedPrivateKey).getEncoded()));
        assertEquals(encodedPrivateKeyBC, Base64.getEncoder().encodeToString(decodePrivateKey(encodedPrivateKeyBC).getEncoded()));
        assertEquals(encodedPublicKey, Base64.getEncoder().encodeToString(decodePublicKey(encodedPublicKey).getEncoded()));

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        assertEquals("BC", KeyFactory.getInstance("EC").getProvider().getName());
        assertNotEquals(encodedPrivateKey, Base64.getEncoder().encodeToString(decodePrivateKey(encodedPrivateKey).getEncoded()));
        assertEquals(encodedPrivateKeyBC, Base64.getEncoder().encodeToString(decodePrivateKey(encodedPrivateKeyBC).getEncoded()));
        assertEquals(encodedPublicKey, Base64.getEncoder().encodeToString(decodePublicKey(encodedPublicKey).getEncoded()));
    }
}
