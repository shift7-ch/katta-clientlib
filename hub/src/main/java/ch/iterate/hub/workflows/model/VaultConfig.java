/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows.model;

import ch.cyberduck.core.cryptomator.CryptoVault;
import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;

import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;

import java.util.HashMap;
import java.util.LinkedHashMap;

import ch.iterate.hub.client.model.ConfigDto;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;


// TODO review @sebi can we use cryptolib instead?
public class VaultConfig {

    // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
    //const jwtPayload: VaultConfigPayload = {
    //    jti: vaultId,
    //    format: 8,
    //    cipherCombo: 'SIV_GCM',
    //    shorteningThreshold: 220
    //};
    //const header = JSON.stringify({
    //    kid: kid,
    //    typ: 'jwt',
    //    alg: 'HS256',
    //    hub: hubConfig
    //});
    public static final CryptoVault.VaultConfig VAULT_CONFIG = new CryptoVault.VaultConfig(8, 220, CryptorProvider.Scheme.SIV_GCM, "HS256", null);

    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/19 remove  createVaultConfig
    @Deprecated
    public static String createVaultConfig(final String vaultId, final String sharedKey, final ConfigDto configDto, final String absBackendBaseURL) throws JOSEException {
        final String clientId = configDto.getKeycloakRealm();

        // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
        //const hubConfig: VaultConfigHeaderHub = {
        //    clientId: cfg.keycloakClientIdCryptomator,
        //    authEndpoint: cfg.keycloakAuthEndpoint,
        //    tokenEndpoint: cfg.keycloakTokenEndpoint,
        //    devicesResourceUrl: `${absBackendBaseURL}devices/`,
        //    authSuccessUrl: `${absFrontendBaseURL}unlock-success?vault=${vaultId}`,
        //    authErrorUrl: `${absFrontendBaseURL}unlock-error?vault=${vaultId}`
        //};
        HashMap<String, Object> customParams = new HashMap<>();
        HashMap<String, Object> hubParams = new LinkedHashMap<>();
        hubParams.put("clientId", clientId);
        hubParams.put("authEndpoint", configDto.getKeycloakAuthEndpoint());
        hubParams.put("tokenEndpoint", configDto.getKeycloakTokenEndpoint());
        hubParams.put("devicesResourceUrl", String.format("%s/api/devices/", absBackendBaseURL));
        hubParams.put("authSuccessUrl", String.format("%s/app/unlock-success?vault=%s", absBackendBaseURL, vaultId));
        hubParams.put("authErrorUrl", String.format("%s/app/unlock-error?vault=%s", absBackendBaseURL, vaultId));
        customParams.put("hub", hubParams);


        final JWSObject jwsObject = new JWSObject(
                // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/crypto.ts
                //const header = JSON.stringify({
                //    kid: kid,
                //    typ: 'jwt',
                //    alg: 'HS256',
                //    hub: hubConfig
                //});
                new JWSHeader(
                        JWSAlgorithm.HS256,
                        JOSEObjectType.JWT,
                        null, null, null, null, null, null, null, null, "hub+" + absBackendBaseURL + "/api/vaults/" + vaultId, true, customParams, null
                ),
                // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/vaultconfig.ts
                //const jwtPayload: VaultConfigPayload = {
                //    jti: vaultId,
                //    format: 8,
                //    cipherCombo: 'SIV_GCM',
                //    shorteningThreshold: 220
                //};
                new Payload("{\n" +
                        "  \"jti\": \"" + vaultId + "\",\n" +
                        "  \"format\": " + VAULT_CONFIG.vaultVersion() + ",\n" +
                        "  \"cipherCombo\": \"" + VAULT_CONFIG.getCipherCombo() + "\",\n" +
                        "  \"shorteningThreshold\": " + VAULT_CONFIG.getShorteningThreshold() + "\n" +
                        "}"));
        // See https://github.com/cryptomator/hub/blob/develop/frontend/src/common/crypto.ts
        //const signature = await crypto.subtle.sign(
        //    'HMAC',
        //    this.masterKey,
        //    encodedUnsignedToken
        //);
        jwsObject.sign(new MACSigner(sharedKey));
        return jwsObject.serialize();
    }

    public static String rootDirHash(final byte[] masterkey) {
        final Cryptor cryptorProvider = CryptorProvider.forScheme(CryptorProvider.Scheme.SIV_GCM).provide(new Masterkey(masterkey), FastSecureRandomProvider.get().provide());
        return cryptorProvider.fileNameCryptor().hashDirectoryId("");
    }
}
