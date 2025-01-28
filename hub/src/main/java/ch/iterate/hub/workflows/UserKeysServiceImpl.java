/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.UUIDRandomStringService;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LocalAccessDeniedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.joda.time.DateTime;

import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.DeviceResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.DeviceDto;
import ch.iterate.hub.client.model.Type1;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.hub.model.SetupCodeJWE;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static ch.iterate.hub.crypto.KeyHelper.getDeviceIdFromDeviceKeyPair;
import static ch.iterate.hub.protocols.s3.CipherduckHostCustomProperties.HUB_UUID;
import static ch.iterate.hub.workflows.DeviceKeysService.COMPUTER_NAME;
import static ch.iterate.hub.workflows.DeviceKeysService.validateDeviceKeys;

public class UserKeysServiceImpl implements UserKeysService {
    private static final Logger log = LogManager.getLogger(UserKeysServiceImpl.class.getName());

    private final UsersResourceApi usersResourceApi;
    private final DeviceResourceApi deviceResourceApi;

    public UserKeysServiceImpl(final HubSession hubSession) {
        this(new UsersResourceApi(hubSession.getClient()), new DeviceResourceApi(hubSession.getClient()));
    }

    public UserKeysServiceImpl(final UsersResourceApi usersResourceApi, final DeviceResourceApi deviceResourceApi) {
        this.usersResourceApi = usersResourceApi;
        this.deviceResourceApi = deviceResourceApi;
    }

    @Override
    public UserKeys getUserKeys(final Host host, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        // Get user key from hub and decrypt with device-keys
        try {
            final UserDto me = usersResourceApi.apiUsersMeGet(true);
            log.info("Retrieved user {}", me);
            final boolean userKeysInHub = validateUserKeys(me);
            log.debug(" -> userKeysInHub={}", userKeysInHub);
            final ECKeyPair deviceKeyPairFromKeychain = new DeviceKeysService().getDeviceKeysFromPasswordStore(me.getId(), host.getProperty(HUB_UUID));
            final boolean deviceKeysInKeychain = validateDeviceKeys(deviceKeyPairFromKeychain);
            log.debug(" -> deviceKeysInKeychain={}", deviceKeysInKeychain);
            if(userKeysInHub && deviceKeysInKeychain) {
                log.info("(1) Get user keys from hub and decrypt with device key from keychain.");

                final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeyPairFromKeychain);
                log.info("(1.1) Got device key pair from keychain with deviceId={}", deviceId);

                try {
                    final String deviceSpecificUserKeys = deviceResourceApi.apiDevicesDeviceIdGet(deviceId).getUserPrivateKey();
                    final UserKeys userKeys = UserKeys.decryptOnDevice(deviceSpecificUserKeys, deviceKeyPairFromKeychain.getPrivate(), me.getEcdhPublicKey(), me.getEcdsaPublicKey());
                    log.info("(1.2) Decrypting device-specific user keys for deviceId={}", deviceId);
                    return userKeys;
                }
                catch(ApiException e) {
                    switch(e.getCode()) {
                        case 404:
                            log.info("(1b) Device keys from keychain not present in hub. Setting up existing device w/ Account Key for existing user keys.");

                            final AccountKeyAndDeviceName accountKeyAndDeviceName = prompt.askForAccountKeyAndDeviceName(host, COMPUTER_NAME);
                            final String setupCode = accountKeyAndDeviceName.accountKey();

                            // Setup existing device w/ Account Key (e.g. same device for multiple hubs)
                            final UserKeys userKeys = UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getEcdsaPublicKey(), setupCode);
                            this.uploadDeviceKeys(accountKeyAndDeviceName.deviceName(), userKeys, deviceKeyPairFromKeychain);
                            return userKeys;
                        default:
                            throw e;
                    }
                }
            }
            else if(userKeysInHub) {
                log.info("(2) Setting up new device w/ Account Key for existing user keys.");

                log.info("(2.1) setup existing device w/ Account Key (e.g. same device for multiple hubs)");
                final AccountKeyAndDeviceName accountKeyAndDeviceName = prompt.askForAccountKeyAndDeviceName(host, COMPUTER_NAME);
                final String setupCode = accountKeyAndDeviceName.accountKey();

                final UserKeys userKeys = UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getPrivateKey(), setupCode);

                log.info("(2.2) Setting up new device w/ Account Key for existing user keys.");
                final ECKeyPair deviceKeyPair = P384KeyPair.generate();

                log.info("(2.3) upload device-specific user keys");
                this.uploadDeviceKeys(accountKeyAndDeviceName.deviceName(), userKeys, deviceKeyPair);

                log.info("(2.4) store new device keys in keychain");
                new DeviceKeysService().storeDeviceKeysInPasswordStore(deviceKeyPair, me.getId(), host.getProperty(HUB_UUID));

                return userKeys;
            }
            else {
                log.info("(3) Setting up new user keys and setupCode.");

                log.info("(3.1) generate and display new Account Key");
                final String setupCode = new UUIDRandomStringService().random();
                log.info("With setupCode={}", setupCode);

                final String deviceName = prompt.displayAccountKeyAndAskDeviceName(host,
                        new AccountKeyAndDeviceName().withAccountKey(setupCode).withDeviceName(COMPUTER_NAME));

                log.info("(3.2) generate user key pair");
                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/27 @sebi private key generated with P384KeyPair causes "Unexpected Error: Data provided to an operation does not meet requirements" in `UserKeys.recover`: `const privateKey = await crypto.subtle.importKey('pkcs8', decodedPrivateKey, UserKeys.KEY_DESIGNATION, false, UserKeys.KEY_USAGES);` WHY?
                final UserKeys userKeys = UserKeys.create();
                // working in hub:
//                userKeys = decodeKeyPair(
//                        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAElHHI0J5tJFdr2+Iz5tEoo7CyLq3CW7AHl/xtPzeGM2rPtNeKkWsN6hK5+2ICeWzIyG2DkbF/jzCiyqK6cMGS+dXykT9o9G4n4lLLpn8dQ4ClBSijm0MPBv2erNo7QcCF",
//                        "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDAPt0LiyP8C7rPJ1f4/QLyyNBUX5SbniyiImd7mtWtRh3qNhBlmMeXWf8px0VGsBnmhZANiAASUccjQnm0kV2vb4jPm0SijsLIurcJbsAeX/G0/N4Yzas+014qRaw3qErn7YgJ5bMjIbYORsX+PMKLKorpwwZL51fKRP2j0bifiUsumfx1DgKUFKKObQw8G/Z6s2jtBwIU=");
                // not working in hub:
//                userKeys = userKeysRecover(
//                        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEmAkfc+lCMuKBkq3pq/eDuhZZb1z2aS/98EM2DLfMBis0QLoXCG0EE9mgnIRYh0yn+oII0vK7FnJGjDdhhfVlfCXgLkg7P52zRt+X6eVWa8UayFkT3qMLlRYNWDAkyaxJ",
//                        "eyJwMnMiOiJicVhoMGJNeS1QS3FmTFBoYW5DNmZ3IiwicDJjIjoxMDAwMDAwLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUEJFUzItSFM1MTIrQTI1NktXIn0.voAnfslMyy4Kydt8yozQLNSNbHCvuzsOLOwx-RFnLaZB1Ft6RFI3Rw.T0HWPxw9ZOyS8UzE.8YdWEf5buapJle9Ji1wYxGDVSxlVl-i6LKEYaRY2HmZPwj9JfaVrFAXrZd_apD_MUTtcfbjjrKcqJmn7ua01ptWCLko3Vy90QUu2xMgzyhS9_aJKAXn23A9VxKPVqTRP3lqzFJn1AYRqc2XiwzJ2pqNg61QSnQ.mfwMD4dsSdQ81BOkHMs9xw",
//                        "blabla");

                log.info("(3.3) upload user keys");
                this.uploadUserKeys(me, userKeys, setupCode);

                log.info("(3.4) Retrieve/generate device keys. Retrieve if present in keychain from another hub.");
                final ECKeyPair deviceKeyPair;
                if(!deviceKeysInKeychain) {
                    log.info("(3.4a) Setting up new device for new user keys.");
                    deviceKeyPair = P384KeyPair.generate();
                }
                else {
                    log.info("(3.4b) Re-using existing device with new user keys.");
                    deviceKeyPair = deviceKeyPairFromKeychain;
                }
                log.info("(3.4) Upload device-specific user keys and store device keys in keychain.");
                this.uploadDeviceKeys(deviceName, userKeys, deviceKeyPair);
                if(!deviceKeysInKeychain) {
                    log.info("(3.5) Store new device keys in keychain");
                    new DeviceKeysService().storeDeviceKeysInPasswordStore(deviceKeyPair, me.getId(), host.getProperty(HUB_UUID));
                }
                return userKeys;
            }
        }
        catch(LocalAccessDeniedException | ConnectionCanceledException e) {
            throw new AccessException(e);
        }
        catch(InvalidKeySpecException | JsonProcessingException | ParseException | JOSEException e) {
            throw new SecurityFailure(e);
        }
    }

    private void uploadUserKeys(final UserDto me, final UserKeys userKeys, final String setupCode) throws ApiException, JOSEException, JsonProcessingException {
        usersResourceApi.apiUsersMePut(me.ecdhPublicKey(userKeys.encodedEcdhPublicKey())
                .ecdsaPublicKey(userKeys.encodedEcdsaPublicKey())
                .privateKey(userKeys.encryptWithSetupCode(setupCode))
                .setupCode(new SetupCodeJWE(setupCode).encryptForUser(userKeys.ecdhKeyPair().getPublic())));
    }

    private void uploadDeviceKeys(final String deviceName, final UserKeys userKeys, final ECKeyPair deviceKeys) throws JOSEException, ApiException, JsonProcessingException {
        final String encodedPublicKeyDeviceKey = Base64.getEncoder().encodeToString(deviceKeys.getPublic().getEncoded());
        final String deviceSpecificUserKeyJWE = userKeys.encryptForDevice(deviceKeys.getPublic());
        final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeys);
        deviceResourceApi.apiDevicesDeviceIdPut(deviceId, new DeviceDto()
                .name(deviceName)
                .publicKey(encodedPublicKeyDeviceKey)
                .userPrivateKey(deviceSpecificUserKeyJWE)
                .type(Type1.DESKTOP)
                .creationTime(new DateTime()));
    }

    private boolean validateUserKeys(final UserDto me) {
        return me.getEcdhPublicKey() != null && me.getPrivateKey() != null;
    }
}
