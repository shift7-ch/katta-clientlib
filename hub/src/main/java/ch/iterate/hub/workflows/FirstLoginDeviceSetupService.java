/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordStore;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LocalAccessDeniedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.common.ECKeyPair;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.joda.time.DateTime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.DeviceResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.DeviceDto;
import ch.iterate.hub.client.model.Type1;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallbackFactory;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.hub.model.SetupCodeJWE;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.FirstLoginDeviceSetupException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static ch.iterate.hub.crypto.KeyHelper.decodeKeyPair;
import static ch.iterate.hub.crypto.KeyHelper.getDeviceIdFromDeviceKeyPair;

// TODO https://github.com/shift7-ch/katta-server/issues/4 unit and integration tests! -> refactor
public class FirstLoginDeviceSetupService {
    private static final Logger log = LogManager.getLogger(FirstLoginDeviceSetupService.class.getName());

    public static final String KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME = "Cipherduck Public Device Key";
    public static final String KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME = "Cipherduck Private Device Key";
    private final HubSession hubSession;

    public FirstLoginDeviceSetupService(final HubSession hubSession) {
        this.hubSession = hubSession;
    }


    private static ECKeyPair getDeviceKeysFromPasswordStore(final String userId, final String hubUUID) throws LocalAccessDeniedException, InvalidKeySpecException {
        final PasswordStore passwordStore = PasswordStoreFactory.get();

        final String encodedPublicDeviceKey = passwordStore.getPassword(String.format(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME), String.format("hub %s - user %s", userId, hubUUID));
        final String encodedPrivateDeviceKey = passwordStore.getPassword(String.format(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME), String.format("hub %s - user %s", userId, hubUUID));
        if((encodedPublicDeviceKey == null) && (encodedPrivateDeviceKey == null)) {
            return null;
        }
        return decodeKeyPair(encodedPublicDeviceKey, encodedPrivateDeviceKey);
    }

    private static void storeDeviceKeysInPasswordStore(final ECKeyPair deviceKeys, final String userId, final String hubUUID) throws LocalAccessDeniedException {
        final PasswordStore passwordStore = PasswordStoreFactory.get();
        passwordStore.addPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, String.format("hub %s - user %s", userId, hubUUID),
                Base64.getEncoder().encodeToString(deviceKeys.getPublic().getEncoded())
        );
        passwordStore.addPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, String.format("hub %s - user %s", userId, hubUUID),
                Base64.getEncoder().encodeToString(deviceKeys.getPrivate().getEncoded())
        );
        log.info("Generated device key pair and stored in keychain.");
    }

    private static String getHostname() throws UnknownHostException {
        final String hostName = InetAddress.getLocalHost().getHostName();
        log.info(String.format("Local host name %s", hostName));
        return hostName;
    }

    // N.B. has side-effect first login!
    public UserKeys getUserKeysWithDeviceKeys() throws ApiException, AccessException, SecurityFailure, FirstLoginDeviceSetupException {

        final UsersResourceApi usersResourceApi = new UsersResourceApi(this.hubSession.getClient());
        final DeviceResourceApi deviceResourceApi = new DeviceResourceApi(this.hubSession.getClient());
        final Host hub = hubSession.getHost();
        UserDto me = usersResourceApi.apiUsersMeGet(true);
        log.info("me before getUserKeysWithDeviceKeys: {}", me);


        final boolean userKeysInHub = (me.getEcdhPublicKey() != null) && (me.getPrivateKey() != null);
        final PasswordStore passwordStore = PasswordStoreFactory.get();
        final boolean deviceKeysInKeychain;
        try {
            deviceKeysInKeychain = (passwordStore.getPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, String.format("hub %s - user %s", me.getId(), hub.getUuid())) != null) &&
                    (passwordStore.getPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, String.format("hub %s - user %s", me.getId(), hub.getUuid())) != null);


            log.info(" -> userKeysInHub={}", userKeysInHub);
            log.info(" -> deviceKeysInKeychain={}", deviceKeysInKeychain);


            if(!userKeysInHub) {
                if((me.getEcdhPublicKey() != null) || (me.getPrivateKey() != null) || (me.getSetupCode() != null)
                ) {
                    throw new FirstLoginDeviceSetupException(String.format("Incomplete first login found: %s. Please go to hub and reset your account.", me));
                }

            }
            if(deviceKeysInKeychain) {
                if((passwordStore.getPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, String.format("hub %s - user %s", me.getId(), hub.getUuid())) == null) ||
                        (passwordStore.getPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, String.format("hub %s - user %s", me.getId(), hub.getUuid())) == null)) {
                    throw new FirstLoginDeviceSetupException(String.format("Keychain contains either only public device key %s or only private device key %s but not both. Please remove device keys manually and setup new device keys with Account Key.", KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME));
                }
            }

            if(userKeysInHub && deviceKeysInKeychain) {
                log.info("(1) Get user keys from hub and decrypt with device key from keychain.");

                final ECKeyPair deviceKeyPair = getDeviceKeysFromPasswordStore(me.getId(), hub.getUuid());

                final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeyPair);
                log.info("(1.1) Got device key pair from keychain with deviceId={}", deviceId);

                try {
                    final String deviceSpecificUserKeys = deviceResourceApi.apiDevicesDeviceIdGet(deviceId).getUserPrivateKey();
                    final UserKeys userKeys = UserKeys.decryptOnDevice(deviceSpecificUserKeys, deviceKeyPair.getPrivate(), me.getEcdhPublicKey(), me.getEcdsaPublicKey());
                    log.info("(1.2) Decrypting device-specific user keys for deviceId={}", deviceId);
                    return userKeys;
                }
                catch(ApiException e) {
                    if(e.getCode() == 404) {
                        log.info("(1b) Device keys from keychain not present in hub. Setting up existing device w/ Account Key for existing user keys.");

                        final AccountKeyAndDeviceName accountKeyAndDeviceName = FirstLoginDeviceSetupCallbackFactory.get().askForAccountKeyAndDeviceName(hub, getHostname());
                        final String setupCode = accountKeyAndDeviceName.accountKey();

                        // Setup existing device w/ Account Key (e.g. same device for multiple hubs)
                        final UserKeys userKeys = UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getEcdsaPublicKey(), setupCode);
                        uploadDeviceKeys(accountKeyAndDeviceName.deviceName(), deviceResourceApi, userKeys, deviceKeyPair);
                        return userKeys;
                    }
                    else {
                        throw e;
                    }
                }
            }
            else if(userKeysInHub && !deviceKeysInKeychain) {
                log.info("(2) Setting up new device w/ Account Key for existing user keys.");

                log.info("(2.1) setup existing device w/ Account Key (e.g. same device for multiple hubs)");
                final AccountKeyAndDeviceName accountKeyAndDeviceName = FirstLoginDeviceSetupCallbackFactory.get().askForAccountKeyAndDeviceName(hub, getHostname());
                final String setupCode = accountKeyAndDeviceName.accountKey();

                final UserKeys userKeys = UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getPrivateKey(), setupCode);

                log.info("(2.2) Setting up new device w/ Account Key for existing user keys.");
                final ECKeyPair deviceKeyPair = P384KeyPair.generate();

                log.info("(2.3) upload device-specific user keys");
                uploadDeviceKeys(accountKeyAndDeviceName.deviceName(), deviceResourceApi, userKeys, deviceKeyPair);

                log.info("(2.4) store new device keys in keychain");
                storeDeviceKeysInPasswordStore(deviceKeyPair, me.getId(), hub.getUuid());

                return userKeys;
            }
            else if(!userKeysInHub) {
                log.info("(3) Setting up new user keys and setupCode.");

                log.info("(3.1) generate and display new Account Key");
                final String accountKey = FirstLoginDeviceSetupCallbackFactory.get().generateAccountKey();
                log.info("With setupCode={}", accountKey);

                final String deviceName = FirstLoginDeviceSetupCallbackFactory.get().displayAccountKeyAndAskDeviceName(hub, new AccountKeyAndDeviceName().withAccountKey(accountKey).withDeviceName(getHostname()));


                log.info("(3.2) generate user key pair");
                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/27 @sebi private key generated with P384KeyPair causes "Unexpected Error: Data provided to an operation does not meet requirements" in `UserKeys.recover`: `const privateKey = await crypto.subtle.importKey('pkcs8', decodedPrivateKey, UserKeys.KEY_DESIGNATION, false, UserKeys.KEY_USAGES);` WHY?
                final UserKeys userKeys = UserKeys.create();
                // working in hub:
//                        userKeys = decodeKeyPair(
//                                "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAElHHI0J5tJFdr2+Iz5tEoo7CyLq3CW7AHl/xtPzeGM2rPtNeKkWsN6hK5+2ICeWzIyG2DkbF/jzCiyqK6cMGS+dXykT9o9G4n4lLLpn8dQ4ClBSijm0MPBv2erNo7QcCF",
//                                "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDAPt0LiyP8C7rPJ1f4/QLyyNBUX5SbniyiImd7mtWtRh3qNhBlmMeXWf8px0VGsBnmhZANiAASUccjQnm0kV2vb4jPm0SijsLIurcJbsAeX/G0/N4Yzas+014qRaw3qErn7YgJ5bMjIbYORsX+PMKLKorpwwZL51fKRP2j0bifiUsumfx1DgKUFKKObQw8G/Z6s2jtBwIU=");
                // not working in hub:
                //            userKeys = userKeysRecover(
                //                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEmAkfc+lCMuKBkq3pq/eDuhZZb1z2aS/98EM2DLfMBis0QLoXCG0EE9mgnIRYh0yn+oII0vK7FnJGjDdhhfVlfCXgLkg7P52zRt+X6eVWa8UayFkT3qMLlRYNWDAkyaxJ",
                //                    "eyJwMnMiOiJicVhoMGJNeS1QS3FmTFBoYW5DNmZ3IiwicDJjIjoxMDAwMDAwLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUEJFUzItSFM1MTIrQTI1NktXIn0.voAnfslMyy4Kydt8yozQLNSNbHCvuzsOLOwx-RFnLaZB1Ft6RFI3Rw.T0HWPxw9ZOyS8UzE.8YdWEf5buapJle9Ji1wYxGDVSxlVl-i6LKEYaRY2HmZPwj9JfaVrFAXrZd_apD_MUTtcfbjjrKcqJmn7ua01ptWCLko3Vy90QUu2xMgzyhS9_aJKAXn23A9VxKPVqTRP3lqzFJn1AYRqc2XiwzJ2pqNg61QSnQ.mfwMD4dsSdQ81BOkHMs9xw",
                //                    "blabla");

                log.info("(3.3) upload user keys");
                me = me.ecdhPublicKey(userKeys.encodedEcdhPublicKey())
                        .ecdsaPublicKey(userKeys.encodedEcdsaPublicKey())
                        .privateKey(userKeys.encryptWithSetupCode(accountKey))
                        .setupCode(new SetupCodeJWE(accountKey).encryptForUser(userKeys.ecdhKeyPair().getPublic()));
                usersResourceApi.apiUsersMePut(me);

                log.info("(3.4) Retrieve/generate device keys. Retrieve if present in keychain from another hub.");
                ECKeyPair deviceKeyPair = getDeviceKeysFromPasswordStore(me.getId(), hub.getUuid());
                boolean generateDeviceKeys = deviceKeyPair == null;
                if(generateDeviceKeys) {
                    log.info("(3.4a) Setting up new device for new user keys.");
                    deviceKeyPair = P384KeyPair.generate();
                }
                else {
                    log.info("(3.4b) Re-using existing device with new user keys.");
                }
                log.info("(3.4) Upload device-specific user keys and store device keys in keychain.");

                uploadDeviceKeys(deviceName, deviceResourceApi, userKeys, deviceKeyPair);

                if(generateDeviceKeys) {
                    log.info("(3.5) Store new device keys in keychain");
                    storeDeviceKeysInPasswordStore(deviceKeyPair, me.getId(), hub.getUuid());
                }
                return userKeys;

            }
            else {
                throw new FirstLoginDeviceSetupException(String.format("Cannot happen userKeysInHub=%s, deviceKeysInKeychain=%s", userKeysInHub, deviceKeysInKeychain));
            }
        }
        catch(LocalAccessDeniedException | UnknownHostException | ConnectionCanceledException e) {
            throw new AccessException(e);
        }
        catch(InvalidKeySpecException | JsonProcessingException | ParseException | JOSEException e) {
            throw new SecurityFailure(e);
        }
    }

    private static void uploadDeviceKeys(final String deviceName, final DeviceResourceApi deviceResourceApi, final UserKeys userKeys, final ECKeyPair deviceKeys) throws JOSEException, ApiException, JsonProcessingException {
        final String encodedPublicKeyDeviceKey = Base64.getEncoder().encodeToString(deviceKeys.getPublic().getEncoded());

        final String deviceSpecificUserKeyJWE = userKeys.encryptForDevice(deviceKeys.getPublic());
        final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeys);
        deviceResourceApi.apiDevicesDeviceIdPut(deviceId,
                new DeviceDto()
                        .name(deviceName)
                        .publicKey(encodedPublicKeyDeviceKey)
                        .userPrivateKey(deviceSpecificUserKeyJWE)
                        .type(Type1.DESKTOP)
                        .creationTime(new DateTime()));
    }
}
