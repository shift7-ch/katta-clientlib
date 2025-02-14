/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.hub.model.SetupCodeJWE;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static ch.iterate.hub.crypto.KeyHelper.getDeviceIdFromDeviceKeyPair;
import static ch.iterate.hub.workflows.DeviceKeysServiceImpl.COMPUTER_NAME;

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
    public UserKeys getUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair) throws ApiException, AccessException, SecurityFailure {
        log.info("Get user keys from {}", hub);
        final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeyPair.getEcKeyPair());
        log.info("Got device key pair from keychain with deviceId={}", deviceId);
        final String deviceSpecificUserKeys = deviceResourceApi.apiDevicesDeviceIdGet(deviceId).getUserPrivateKey();
        final UserKeys userKeys;
        try {
            userKeys = UserKeys.decryptOnDevice(deviceSpecificUserKeys, deviceKeyPair.getEcKeyPair().getPrivate(), me.getEcdhPublicKey(), me.getEcdsaPublicKey());
        }
        catch(ParseException | JOSEException | InvalidKeySpecException e) {
            throw new SecurityFailure(e);
        }
        log.info("Decrypting device-specific user keys for deviceId={}", deviceId);
        return userKeys;
    }

    @Override
    public UserKeys getOrCreateUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        if(UserKeys.validate(me)) {
            try {
                return this.getUserKeys(hub, me, deviceKeyPair);
            }
            catch(ApiException e) {
                switch(e.getCode()) {
                    case 404:
                        log.warn("Device keys from keychain not present in hub. Setting up existing device w/ Account Key for existing user keys.");
                        // Setup existing device w/ Account Key (e.g. same device for multiple hubs)
                        return this.recover(me, deviceKeyPair, prompt.askForAccountKeyAndDeviceName(hub, COMPUTER_NAME));
                    default:
                        throw e;
                }
            }
        }
        else if(UserKeys.validate(me)) {
            // No device keys
            log.info("Setting up new device w/ Account Key for existing user keys.");
            return this.recover(me, deviceKeyPair, prompt.askForAccountKeyAndDeviceName(hub, COMPUTER_NAME));
        }
        else {
            log.info("Setting up new user keys and account key");

            // TODO https://github.com/shift7-ch/katta-server/issues/27
            // private key generated with P384KeyPair causes "Unexpected Error: Data provided to an operation does not meet requirements" in `UserKeys.recover`: `const privateKey = await crypto.subtle.importKey('pkcs8', decodedPrivateKey, UserKeys.KEY_DESIGNATION, false, UserKeys.KEY_USAGES);`
            final String accountKey = prompt.generateAccountKey();
            final String deviceName = prompt.displayAccountKeyAndAskDeviceName(hub,
                    new AccountKeyAndDeviceName().withAccountKey(accountKey).withDeviceName(COMPUTER_NAME));

            return this.uploadDeviceKeys(deviceName,
                    this.uploadUserKeys(me, prompt.generateUserKeys(), accountKey), deviceKeyPair);
        }
    }

    private UserKeys recover(final UserDto me, final DeviceKeys deviceKeyPair, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws
            ApiException, SecurityFailure {
        try {
            return this.uploadDeviceKeys(accountKeyAndDeviceName.deviceName(),
                    UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getEcdsaPublicKey(), accountKeyAndDeviceName.accountKey()), deviceKeyPair);
        }
        catch(ParseException | JOSEException | InvalidKeySpecException e) {
            throw new SecurityFailure(e);
        }
    }

    private UserKeys uploadUserKeys(final UserDto me, final UserKeys userKeys, final String setupCode) throws ApiException, SecurityFailure {
        try {
            usersResourceApi.apiUsersMePut(me.ecdhPublicKey(userKeys.encodedEcdhPublicKey())
                    .ecdsaPublicKey(userKeys.encodedEcdsaPublicKey())
                    .privateKey(userKeys.encryptWithSetupCode(setupCode))
                    .setupCode(new SetupCodeJWE(setupCode).encryptForUser(userKeys.ecdhKeyPair().getPublic())));
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
        return userKeys;
    }

    private UserKeys uploadDeviceKeys(final String deviceName, final UserKeys userKeys, final DeviceKeys deviceKeys) throws ApiException, SecurityFailure {
        try {
            final String encodedPublicKeyDeviceKey = Base64.getEncoder().encodeToString(deviceKeys.getEcKeyPair().getPublic().getEncoded());
            final String deviceSpecificUserKeyJWE = userKeys.encryptForDevice(deviceKeys.getEcKeyPair().getPublic());
            final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeys.getEcKeyPair());
            deviceResourceApi.apiDevicesDeviceIdPut(deviceId, new DeviceDto()
                    .name(deviceName)
                    .publicKey(encodedPublicKeyDeviceKey)
                    .userPrivateKey(deviceSpecificUserKeyJWE)
                    .type(Type1.DESKTOP)
                    .creationTime(new DateTime()));
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
        return userKeys;
    }
}
