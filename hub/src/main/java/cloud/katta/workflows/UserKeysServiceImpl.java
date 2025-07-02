/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.BookmarkNameProvider;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordStore;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.exception.LocalAccessDeniedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.DeviceResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.DeviceDto;
import cloud.katta.client.model.Type1;
import cloud.katta.client.model.UserDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.AccountKeyAndDeviceName;
import cloud.katta.model.SetupCodeJWE;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static cloud.katta.crypto.KeyHelper.getDeviceIdFromDeviceKeyPair;
import static cloud.katta.workflows.DeviceKeysServiceImpl.COMPUTER_NAME;

public class UserKeysServiceImpl implements UserKeysService {
    private static final Logger log = LogManager.getLogger(UserKeysServiceImpl.class.getName());

    private final UsersResourceApi usersResourceApi;
    private final DeviceResourceApi deviceResourceApi;

    private final PasswordStore store;

    public UserKeysServiceImpl(final HubSession hubSession) {
        this(hubSession, PasswordStoreFactory.get());
    }

    public UserKeysServiceImpl(final HubSession hubSession, PasswordStore store) {
        this(new UsersResourceApi(hubSession.getClient()), new DeviceResourceApi(hubSession.getClient()), store);
    }

    public UserKeysServiceImpl(final UsersResourceApi usersResourceApi, final DeviceResourceApi deviceResourceApi) {
        this(usersResourceApi, deviceResourceApi, PasswordStoreFactory.get());
    }

    public UserKeysServiceImpl(final UsersResourceApi usersResourceApi, final DeviceResourceApi deviceResourceApi, PasswordStore store) {
        this.usersResourceApi = usersResourceApi;
        this.deviceResourceApi = deviceResourceApi;
        this.store = store;
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
    public UserKeys getOrCreateUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair, final DeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        if(validate(me)) {
            try {
                return this.getUserKeys(hub, me, deviceKeyPair);
            }
            catch(ApiException e) {
                switch(e.getCode()) {
                    case 404:
                        log.warn("Device keys from keychain not present in hub. Setting up existing device w/ Account Key for existing user keys.");
                        // Setup existing device w/ Account Key (e.g. same device for multiple hubs)
                        final AccountKeyAndDeviceName input = prompt.askForAccountKeyAndDeviceName(hub, COMPUTER_NAME);
                        if(input.addToKeychain()) {
                            this.save(hub, me, input.accountKey());
                        }
                        return this.recover(me, deviceKeyPair, input);
                    default:
                        throw e;
                }
            }
        }
        else if(validate(me)) {
            // No device keys
            log.info("Setting up new device w/ Account Key for existing user keys.");
            return this.recover(me, deviceKeyPair, prompt.askForAccountKeyAndDeviceName(hub, COMPUTER_NAME));
        }
        else {
            log.info("Setting up new user keys and account key");

            // TODO https://github.com/shift7-ch/katta-server/issues/27
            // private key generated with P384KeyPair causes "Unexpected Error: Data provided to an operation does not meet requirements" in `UserKeys.recover`: `const privateKey = await crypto.subtle.importKey('pkcs8', decodedPrivateKey, UserKeys.KEY_DESIGNATION, false, UserKeys.KEY_USAGES);`
            final String accountKey = prompt.generateAccountKey();
            final AccountKeyAndDeviceName input = prompt.displayAccountKeyAndAskDeviceName(hub,
                    new AccountKeyAndDeviceName().withAccountKey(accountKey).withDeviceName(COMPUTER_NAME));
            if(input.addToKeychain()) {
                this.save(hub, me, accountKey);
            }
            return this.uploadDeviceKeys(input.deviceName(),
                    this.uploadUserKeys(me, prompt.generateUserKeys(), accountKey), deviceKeyPair);
        }
    }

    private void save(final Host hub, final UserDto me, final String accountKey) {
        try {
            store.addPassword(BookmarkNameProvider.toString(hub), me.getEmail(), accountKey);
        }
        catch(LocalAccessDeniedException ex) {
            log.warn("Failure saving account key", ex);
        }
    }

    private UserKeys recover(final UserDto me, final DeviceKeys deviceKeyPair, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws
            ApiException, SecurityFailure {
        try {
            return this.uploadDeviceKeys(accountKeyAndDeviceName.deviceName(),
                    UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getPrivateKey(), accountKeyAndDeviceName.accountKey()), deviceKeyPair);
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

    private static boolean validate(final UserDto me) {
        return me.getEcdhPublicKey() != null && me.getPrivateKey() != null;
    }
}
