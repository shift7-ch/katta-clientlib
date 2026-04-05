/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.P384KeyPair;

import javax.security.auth.Destroyable;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.Base64;
import java.util.Objects;

import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static cloud.katta.crypto.KeyHelper.decodeKeyPair;
import static cloud.katta.crypto.UserKeyPayload.createFromPayload;

/**
 * Represents Cryptomator Hub <a href="https://docs.cryptomator.org/en/latest/security/hub/#user-key-pair>User Keys</a>.
 * Counterpart of <a href="https://github.com/cryptomator/hub/blob/develop/frontend/src/common/crypto.ts"><code>UserKeys</code></a>.
 */
public class UserKeys implements Destroyable {

    private final ECKeyPair ecdhKeyPair;
    private final ECKeyPair ecdsaKeyPair;

    public UserKeys(final ECKeyPair ecdhKeyPair, final ECKeyPair ecdsaKeyPair) {
        this.ecdhKeyPair = ecdhKeyPair;
        this.ecdsaKeyPair = ecdsaKeyPair;
    }

    public ECKeyPair ecdhKeyPair() {
        return ecdhKeyPair;
    }

    public ECKeyPair ecdsaKeyPair() {
        return ecdsaKeyPair;
    }

    @Override
    public void destroy() {
        ecdhKeyPair.destroy();
        ecdsaKeyPair.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return ecdhKeyPair.isDestroyed() || ecdsaKeyPair.isDestroyed();
    }

    @Override
    public final boolean equals(final Object o) {
        if(!(o instanceof UserKeys)) {
            return false;
        }

        UserKeys userKeys = (UserKeys) o;
        return Objects.equals(ecdhKeyPair, userKeys.ecdhKeyPair) && Objects.equals(ecdsaKeyPair, userKeys.ecdsaKeyPair);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(ecdhKeyPair);
        result = 31 * result + Objects.hashCode(ecdsaKeyPair);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserKeys{");
        sb.append("ecdhKeyPair=").append(ecdhKeyPair);
        sb.append(", ecdsaKeyPair=").append(ecdsaKeyPair);
        sb.append('}');
        return sb.toString();
    }

    public static UserKeys create() {
        return new UserKeys(P384KeyPair.generate(), P384KeyPair.generate());
    }

    private UserKeyPayload prepareForEncryption() {
        return new UserKeyPayload(
                Base64.getEncoder().encodeToString(ecdhKeyPair().getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(ecdsaKeyPair().getPrivate().getEncoded()));
    }

    /**
     * Gets the base64-encoded ECDH public key in SPKI format.
     *
     * @return base64-encoded public key
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.encodedEcdhPublicKey</a>
     */
    public String encodedEcdhPublicKey() {
        return Base64.getEncoder().encodeToString(ecdhKeyPair().getPublic().getEncoded());
    }

    /**
     * Gets the base64-encoded ECDSA public key in SPKI format.
     *
     * @return base64-encoded public key
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.encodedEcdsaPublicKey</a>
     */
    public String encodedEcdsaPublicKey() {
        return Base64.getEncoder().encodeToString(ecdsaKeyPair().getPublic().getEncoded());
    }

    /**
     * Re-create the user key pairs from JWE.
     *
     * @param jwe                JWE containing the PKCS#8-encoded private key
     * @param userEcdhPublicKey  User's public ECDH key
     * @param userEcdsaPublicKey User's public ECDSA key (will be generated if missing - added in Hub 1.4.0)
     * @return The user's key pair
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.createFromJwe</a>
     */
    public static UserKeys createFromJwe(final UserKeyPayload jwe, final String userEcdhPublicKey, final String userEcdsaPublicKey) throws SecurityFailure {
        return new UserKeys(decodeKeyPair(userEcdhPublicKey, jwe.ecdhPrivateKey()), decodeKeyPair(userEcdsaPublicKey, jwe.ecdsaPrivateKey()));
    }

    /**
     * Encrypts the user's private key using a key derived from the given setupCode.
     *
     * @param accountKey The setup code to protect the private key.
     * @return A JWE holding the encrypted private key
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.encryptWithSetupCode</a>
     */
    public String encryptWithAccountKey(final String accountKey) throws SecurityFailure {
        try {
            return JWE.pbes2Encrypt(prepareForEncryption(), "org.cryptomator.hub.setupCode", accountKey);
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }

    /**
     * Recovers the user key pair using a recovery code. All other information can be retrieved from the backend.
     *
     * @param userPrivateKey        JWE containing the PKCS#8-encoded private key
     * @param accountKey            The setup code used to protect the private keys
     * @param encodedEcdhPublicKey  The ECDH public key (base64-encoded SPKI)
     * @param encodedEcdsaPublicKey The ECDSA public key (base64-encoded SPKI)
     * @return Decrypted UserKeys
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.recover()</a>
     */
    public static UserKeys recoverWithAccountKey(final String userPrivateKey, final String accountKey, final String encodedEcdhPublicKey, final String encodedEcdsaPublicKey) throws SecurityFailure {
        try {
            return createFromJwe(createFromPayload(JWE.decryptPbes2(userPrivateKey, accountKey)), encodedEcdhPublicKey, encodedEcdsaPublicKey);
        }
        catch(ParseException | JOSEException e) {
            throw new SecurityFailure(e);
        }
    }

    /**
     * Encrypts the user's private key using the given public key
     *
     * @param devicePublicKey The device's public key (DER-encoded)
     * @return a JWE containing the PKCS#8-encoded private key
     */
    public String encryptForDevice(final ECPublicKey devicePublicKey) throws SecurityFailure {
        try {
            return JWE.ecdhEsEncrypt(prepareForEncryption(), "org.cryptomator.hub.deviceKey", devicePublicKey);
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }

    /**
     * Decrypts the user's private key using the device private key
     *
     * @param userPrivateKey     JWE containing the PKCS#8-encoded private key
     * @param devicePrivateKey   Device private key
     * @param userEcdhPublicKey  User public ECDH key
     * @param userEcdsaPublicKey User public ECDSA key
     * @return The user's key pair
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.decryptOnBrowser()</a>
     */
    public static UserKeys decryptOnDevice(final String userPrivateKey, final ECPrivateKey devicePrivateKey, final String userEcdhPublicKey, final String userEcdsaPublicKey) throws SecurityFailure {
        try {
            return createFromJwe(createFromPayload(JWE.decryptEcdhEs(userPrivateKey, devicePrivateKey)), userEcdhPublicKey, userEcdsaPublicKey);
        }
        catch(ParseException | JOSEException e) {
            throw new SecurityFailure(e);
        }
    }

    /**
     * Decrypts the access token using the user's ECDH private key
     *
     * @param vaultAccessTokenPayload The encrypted access token
     * @return The token's payload
     * @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/crypto.ts">crypto.ts/UserKeys.decryptAccessToken()</a>
     */
    public UVFAccessTokenPayload decryptAccessToken(final String vaultAccessTokenPayload) throws SecurityFailure {
        try {
            return UVFAccessTokenPayload.fromJWE(JWE.decryptEcdhEs(vaultAccessTokenPayload, this.ecdhKeyPair().getPrivate()).toString());
        }
        catch(JsonProcessingException | JOSEException | ParseException e) {
            throw new SecurityFailure(e);
        }
    }
}
