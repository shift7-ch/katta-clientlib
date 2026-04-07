/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Host;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.Base64;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.DeviceResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.DeviceDto;
import cloud.katta.client.model.Type1;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.WithCounts;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.AccountKeyPayload;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static cloud.katta.crypto.KeyHelper.getDeviceIdFromDeviceKeyPair;

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
    public UserKeys getUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair) throws ApiException, SecurityFailure {
        log.info("Get user keys from {}", hub);
        final String deviceId = getDeviceIdFromDeviceKeyPair(deviceKeyPair.getEcKeyPair());
        log.info("Got device key pair from keychain with deviceId={}", deviceId);
        final String deviceSpecificUserKeys = deviceResourceApi.apiDevicesDeviceIdGet(deviceId).getUserPrivateKey();
        final UserKeys userKeys = UserKeys.decryptOnDevice(deviceSpecificUserKeys, deviceKeyPair.getEcKeyPair().getPrivate(), me.getEcdhPublicKey(), me.getEcdsaPublicKey());
        log.info("Decrypting device-specific user keys for deviceId={}", deviceId);
        return userKeys;
    }

    @Override
    public UserKeys getOrCreateUserKeys(final Host bookmark, final UserDto me, final DeviceKeys deviceKeyPair, final DeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        if(validate(me)) {
            try {
                return this.getUserKeys(bookmark, me, deviceKeyPair);
            }
            catch(ApiException e) {
                switch(e.getCode()) {
                    case 404:
                        log.warn("Device keys from keychain not found on server. Setting up existing device with account key for existing user keys.");
                        return this.recoverUserKeys(bookmark, me, deviceKeyPair, prompt);
                    default:
                        throw e;
                }
            }
        }
        // First login: No user nor device keys
        log.info("Setting up new user keys and account key");
        final String accountKey = prompt.generateAccountKey();
        final DeviceSetupCallback.AccountKeyAndDeviceName input = prompt.displayAccountKeyAndAskDeviceName(bookmark, accountKey);
        final UserKeys userKey = prompt.generateUserKeys();
        this.uploadUserKeys(me, userKey, accountKey);
        this.uploadDeviceKeys(input.deviceName(), userKey, deviceKeyPair);
        return userKey;
    }

    /**
     * @throws AccessException Canceled prompt by user
     */
    private UserKeys recoverUserKeys(final Host bookmark, final UserDto me, final DeviceKeys deviceKeyPair, final DeviceSetupCallback prompt) throws AccessException {
        // Setup existing device with account key
        final DeviceSetupCallback.AccountKeyAndDeviceName input = prompt.askForAccountKeyAndDeviceName(bookmark);
        try {
            final UserKeys userKeys = UserKeys.recoverWithAccountKey(me.getPrivateKey(), input.accountKey(),
                    me.getEcdhPublicKey(), me.getEcdsaPublicKey());
            this.uploadDeviceKeys(input.deviceName(), userKeys, deviceKeyPair);
            return userKeys;
        }
        catch(SecurityFailure | ApiException f) {
            log.warn("Failure {} to recover user keys with account key", f.getMessage());
            // Repeat until canceled by user
            return recoverUserKeys(bookmark, me, deviceKeyPair, prompt);
        }
    }

    private void uploadUserKeys(final UserDto me, final UserKeys userKeys, final String accountKey) throws ApiException, SecurityFailure {
        try {
            usersResourceApi.apiUsersMePut(me.ecdhPublicKey(userKeys.encodedEcdhPublicKey())
                    .ecdsaPublicKey(userKeys.encodedEcdsaPublicKey())
                    .privateKey(userKeys.encryptWithAccountKey(accountKey))
                    .setupCode(new AccountKeyPayload(accountKey).encryptForUser(userKeys.ecdhKeyPair().getPublic())));
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }

    private void uploadDeviceKeys(final String deviceName, final UserKeys userKeys, final DeviceKeys deviceKeys) throws ApiException, SecurityFailure {
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

    private static boolean validate(final UserDto me) {
        return me.getEcdhPublicKey() != null && me.getPrivateKey() != null;
    }

    public static UserDto withCountsToUserDto(WithCounts withCounts) {
        return new UserDto()
                .id(withCounts.getId())
                .firstName(withCounts.getFirstName())
                .lastName(withCounts.getLastName())
                .name(withCounts.getName())
                .ecdhPublicKey(withCounts.getEcdhPublicKey())
                .ecdsaPublicKey(withCounts.getEcdsaPublicKey())
                .devices(withCounts.getDevices())
                .language(withCounts.getLanguage())
                .email(withCounts.getEmail())
                .realmRoles(withCounts.getRealmRoles())
                .pictureUrl(withCounts.getPictureUrl())
                .privateKeys(withCounts.getPrivateKeys())
                .setupCode(withCounts.getSetupCode())
                .type(withCounts.getType());
    }

}
