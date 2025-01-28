/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.wot.SignedKeys;
import ch.iterate.hub.crypto.wot.WoT;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

import static ch.iterate.hub.crypto.KeyHelper.encodePublicKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

class WoTServiceImplTest {

    @Test
    void testGetTrustLevelsPerUserId() throws ParseException, JOSEException, ApiException, AccessException, SecurityFailure {
        final List<String> bobSignatureChain = new LinkedList<>();
        int len = 5;

        final UserKeys bobKeys = UserKeys.create();
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("bob")
                .ecdhPublicKey(encodePublicKey(bobKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobKeys.ecdsaKeyPair().getPublic()));
        UserDto previousUser = bob;
        UserKeys previousKeys = bobKeys;

        for(int i = 0; i < len; i++) {
            final UserKeys userKeys = UserKeys.create();
            final UserDto user = new UserDto()
                    .id(UUID.randomUUID().toString())
                    .name(String.format("user%s", i))
                    .ecdhPublicKey(encodePublicKey(userKeys.ecdhKeyPair().getPublic()))
                    .ecdsaPublicKey(encodePublicKey(userKeys.ecdsaKeyPair().getPublic()));
            final String signature = WoT.sign(userKeys.ecdsaKeyPair().getPrivate(), user.getId(), previousUser);
            bobSignatureChain.add(0, signature);

            previousUser = user;
            previousKeys = userKeys;
        }
        final UserDto alice = previousUser;
        final UserKeys aliceKeys = previousKeys;

        final UserKeys oscarKeys = UserKeys.create();
        final UserDto oscar = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("oscar")
                .ecdhPublicKey(encodePublicKey(oscarKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(oscarKeys.ecdsaKeyPair().getPublic()));

        // valid signature chain
        final TrustedUserDto bobTrust = new TrustedUserDto();
        bobTrust.setTrustedUserId(bob.getId());
        bobTrust.setSignatureChain(bobSignatureChain);

        // invalid signature chain
        final TrustedUserDto oscarTrust = new TrustedUserDto();
        oscarTrust.setTrustedUserId(bob.getId());
        oscarTrust.setSignatureChain(bobSignatureChain);

        final UsersResourceApi usersMock = Mockito.mock(UsersResourceApi.class);
        final WoTServiceImpl wot = new WoTServiceImpl(usersMock);
        Mockito.when(usersMock.apiUsersMeGet(true)).thenReturn(alice);
        Mockito.when(usersMock.apiUsersGet()).thenReturn(Arrays.asList(alice, bob, oscar));
        Mockito.when(usersMock.apiUsersTrustedGet()).thenReturn(Arrays.asList(bobTrust, oscarTrust));

        assertEquals(Collections.singletonMap(bob.getId(), 5), wot.getTrustLevelsPerUserId(aliceKeys));
    }

    @Test
    void testVerify() throws ParseException, JOSEException, ApiException, AccessException, SecurityFailure {
        final List<String> signatureChain = new LinkedList<>();
        int len = 5;

        final UserKeys bobKeys = UserKeys.create();
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("bob")
                .ecdhPublicKey(encodePublicKey(bobKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobKeys.ecdsaKeyPair().getPublic()));

        UserKeys previousKeys = bobKeys;
        UserDto previousUser = bob;
        for(int i = 0; i < len; i++) {
            final UserKeys userKeys = UserKeys.create();
            final UserDto user = new UserDto()
                    .id(UUID.randomUUID().toString())
                    .name(String.format("user%s", i))
                    .ecdhPublicKey(encodePublicKey(userKeys.ecdhKeyPair().getPublic()))
                    .ecdsaPublicKey(encodePublicKey(userKeys.ecdsaKeyPair().getPublic()));
            final String signature = WoT.sign(userKeys.ecdsaKeyPair().getPrivate(), user.getId(), previousUser);
            signatureChain.add(0, signature);

            previousKeys = userKeys;
            previousUser = user;
        }
        final UserDto alice = previousUser;
        final UserKeys aliceKeys = previousKeys;

        final UsersResourceApi usersMock = Mockito.mock(UsersResourceApi.class);
        Mockito.when(usersMock.apiUsersMeGet(true)).thenReturn(alice);

        final WoTServiceImpl wot = new WoTServiceImpl(usersMock);
        wot.verify(aliceKeys, signatureChain, SignedKeys.fromUser(bob));
        assertThrows(SecurityFailure.class, () -> wot.verify(aliceKeys, signatureChain, SignedKeys.fromUser(alice)));
    }

    @Test
    void testSign() throws ApiException, ParseException, JOSEException, AccessException, SecurityFailure {
        final UserKeys aliceKeys = UserKeys.create();
        final P384KeyPair bobEcdhKeys = P384KeyPair.generate();
        final P384KeyPair bobEcdsaKeys = P384KeyPair.generate();
        final UserDto alice = new UserDto().id(UUID.randomUUID().toString());
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(bobEcdhKeys.getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobEcdsaKeys.getPublic()));

        final String expectedSignature = WoT.sign(aliceKeys.ecdsaKeyPair().getPrivate(), alice.getId(), bob);

        final UsersResourceApi usersMock = Mockito.mock(UsersResourceApi.class);
        Mockito.when(usersMock.apiUsersMeGet(true)).thenReturn(alice);
        final WoTServiceImpl wot = new WoTServiceImpl(usersMock);
        final TrustedUserDto expectedTrust = new TrustedUserDto().trustedUserId(bob.getId()).signatureChain(Collections.singletonList(expectedSignature));
        Mockito.when(usersMock.apiUsersTrustedUserIdGet(bob.getId())).thenReturn(expectedTrust);

        final TrustedUserDto trust = wot.sign(aliceKeys, bob);
        Mockito.verify(usersMock, times(1)).apiUsersTrustedUserIdPut(eq(bob.getId()), anyString());
        assertEquals(expectedTrust, trust);
    }
}
