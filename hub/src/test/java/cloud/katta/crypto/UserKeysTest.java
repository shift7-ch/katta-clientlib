/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;

import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static cloud.katta.crypto.KeyHelper.decodeKeyPair;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class UserKeysTest {

    @Test
    public void testEncryptWithSetupCodeAndRecover() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        // PEM-encoded
        final String encodedPublicKey = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESA0oPUI0DrULBpIjTRckRduqaPqKz2f4zF6UvB+WHyOVZNsWZHHWIdjZ4LkoOygNenLgllv/iyzzVrH2ILR3Si6s03UOnicBbLy8jPY3MRvdgJPNz4C0kFa7HNXtQNKE";
        // pkcs8-encoded
        // N.B. BC returns encoding with additional information, SunEC returns same encoding, see KeyHelperTest#testElliptic
        // final String encodedPrivateKey = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCwqDLejJNvg83KP+zeNuLcrHnGjABYZXDq4NNxwHPtwv12vHDghCng4vM+qmN+EzWhZANiAARIDSg9QjQOtQsGkiNNFyRF26po+orPZ/jMXpS8H5YfI5Vk2xZkcdYh2NnguSg7KA16cuCWW/+LLPNWsfYgtHdKLqzTdQ6eJwFsvLyM9jcxG92Ak83PgLSQVrsc1e1A0oQ=";
        final String encodedPrivateKey = "MIG/AgEAMBAGByqGSM49AgEGBSuBBAAiBIGnMIGkAgEBBDCwqDLejJNvg83KP+zeNuLcrHnGjABYZXDq4NNxwHPtwv12vHDghCng4vM+qmN+EzWgBwYFK4EEACKhZANiAARIDSg9QjQOtQsGkiNNFyRF26po+orPZ/jMXpS8H5YfI5Vk2xZkcdYh2NnguSg7KA16cuCWW/+LLPNWsfYgtHdKLqzTdQ6eJwFsvLyM9jcxG92Ak83PgLSQVrsc1e1A0oQ=";

        final UserKeys userKeys = new UserKeys(decodeKeyPair(encodedPublicKey, encodedPrivateKey), decodeKeyPair(encodedPublicKey, encodedPrivateKey));
        final String encryptedPrivateKey = userKeys.encryptWithSetupCode("top secret");
        final UserKeys userKeysRecovered = UserKeys.recover(encodedPublicKey, encodedPublicKey, encryptedPrivateKey, "top secret");
        assertArrayEquals(Base64.getDecoder().decode(encodedPrivateKey), userKeysRecovered.ecdhKeyPair().getPrivate().getEncoded());
        assertArrayEquals(Base64.getDecoder().decode(encodedPrivateKey), userKeysRecovered.ecdsaKeyPair().getPrivate().getEncoded());
    }

    @Test
    public void testEncryptWithSetupCodeAndRecoverNew() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        final UserKeys userKeys = UserKeys.create();

        final String encryptedPrivateKey = userKeys.encryptWithSetupCode("top secret");

        final UserKeys userKeysRecovered = UserKeys.recover(userKeys.encodedEcdhPublicKey(), userKeys.encodedEcdsaPublicKey(), encryptedPrivateKey, "top secret");

        assertArrayEquals(userKeys.ecdhKeyPair().getPrivate().getEncoded(), userKeysRecovered.ecdhKeyPair().getPrivate().getEncoded());
        assertArrayEquals(userKeys.ecdsaKeyPair().getPrivate().getEncoded(), userKeysRecovered.ecdsaKeyPair().getPrivate().getEncoded());
    }

    @Test
    public void testEncrypForDeviceAndDecryptOnDevice() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        // PEM-encoded
        final String encodedPublicKey = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESA0oPUI0DrULBpIjTRckRduqaPqKz2f4zF6UvB+WHyOVZNsWZHHWIdjZ4LkoOygNenLgllv/iyzzVrH2ILR3Si6s03UOnicBbLy8jPY3MRvdgJPNz4C0kFa7HNXtQNKE";
        // pkcs8-encoded
        // N.B. BC returns encoding with additional information, SunEC returns same encoding, see KeyHelperTest#testElliptic
        // final String encodedPrivateKey = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCwqDLejJNvg83KP+zeNuLcrHnGjABYZXDq4NNxwHPtwv12vHDghCng4vM+qmN+EzWhZANiAARIDSg9QjQOtQsGkiNNFyRF26po+orPZ/jMXpS8H5YfI5Vk2xZkcdYh2NnguSg7KA16cuCWW/+LLPNWsfYgtHdKLqzTdQ6eJwFsvLyM9jcxG92Ak83PgLSQVrsc1e1A0oQ=";
        final String encodedPrivateKey = "MIG/AgEAMBAGByqGSM49AgEGBSuBBAAiBIGnMIGkAgEBBDCwqDLejJNvg83KP+zeNuLcrHnGjABYZXDq4NNxwHPtwv12vHDghCng4vM+qmN+EzWgBwYFK4EEACKhZANiAARIDSg9QjQOtQsGkiNNFyRF26po+orPZ/jMXpS8H5YfI5Vk2xZkcdYh2NnguSg7KA16cuCWW/+LLPNWsfYgtHdKLqzTdQ6eJwFsvLyM9jcxG92Ak83PgLSQVrsc1e1A0oQ=";

        final UserKeys userKeys = new UserKeys(decodeKeyPair(encodedPublicKey, encodedPrivateKey), decodeKeyPair(encodedPublicKey, encodedPrivateKey));

        final P384KeyPair deviceKeys = P384KeyPair.generate();

        final String jwe = userKeys.encryptForDevice(deviceKeys.getPublic());

        final UserKeys userKeysRecovered = UserKeys.decryptOnDevice(jwe, deviceKeys.getPrivate(), encodedPublicKey, encodedPublicKey);
        assertArrayEquals(Base64.getDecoder().decode(encodedPrivateKey), userKeysRecovered.ecdhKeyPair().getPrivate().getEncoded());
        assertArrayEquals(Base64.getDecoder().decode(encodedPrivateKey), userKeysRecovered.ecdsaKeyPair().getPrivate().getEncoded());
    }

    @Test
    public void testEncrypForDeviceAndDecryptOnDeviceNew() throws ParseException, JOSEException, JsonProcessingException, InvalidKeySpecException {
        final P384KeyPair deviceKeys = P384KeyPair.generate();
        final UserKeys userKeys = UserKeys.create();

        final String jwe = userKeys.encryptForDevice(deviceKeys.getPublic());

        final UserKeys userKeysRecovered = UserKeys.decryptOnDevice(jwe, deviceKeys.getPrivate(), userKeys.encodedEcdhPublicKey(), userKeys.encodedEcdsaPublicKey());

        assertArrayEquals(userKeys.ecdhKeyPair().getPrivate().getEncoded(), userKeysRecovered.ecdhKeyPair().getPrivate().getEncoded());
        assertArrayEquals(userKeys.ecdsaKeyPair().getPrivate().getEncoded(), userKeysRecovered.ecdsaKeyPair().getPrivate().getEncoded());
    }
}
