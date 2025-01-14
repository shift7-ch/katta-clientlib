/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.crypto.wot;

import ch.cyberduck.core.Host;

import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.exceptions.NotECKeyException;
import ch.iterate.hub.workflows.UserKeysService;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;

import static ch.iterate.hub.crypto.KeyHelper.decodePublicKey;
import static ch.iterate.hub.crypto.KeyHelper.encodePublicKey;
import static org.junit.jupiter.api.Assertions.*;

class WoTTest {

    @Test
    void sign() throws ParseException, JOSEException {
        final UserKeys aliceKeys = UserKeys.create();
        final P384KeyPair bobEcdhKeys = P384KeyPair.generate();
        final P384KeyPair bobEcdsaKeys = P384KeyPair.generate();
        final UserDto alice = new UserDto().id(UUID.randomUUID().toString());
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(bobEcdhKeys.getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobEcdsaKeys.getPublic()));

        final String signature = WoT.sign(aliceKeys.ecdsaKeyPair().getPrivate(), alice.getId(), bob);
        final SignedJWT jwt = SignedJWT.parse(signature);
        assertEquals(JWSAlgorithm.ES384, jwt.getHeader().getAlgorithm());
        assertEquals(JOSEObjectType.JWT, jwt.getHeader().getType());
        assertTrue(jwt.getHeader().isBase64URLEncodePayload());
        assertEquals(alice.getId(), jwt.getHeader().getCustomParam("iss"));
        assertEquals(bob.getId(), jwt.getHeader().getCustomParam("sub"));
        assertNotNull(jwt.getHeader().getCustomParam("iat"));
        final Map<String, Object> expectedClaimSet = ImmutableMap.<String, Object>builder()
                .put("ecdhPublicKey", encodePublicKey(bobEcdhKeys.getPublic()))
                .put("ecdsaPublicKey", encodePublicKey(bobEcdsaKeys.getPublic()))
                .build();
        assertEquals(expectedClaimSet, jwt.getJWTClaimsSet().getClaims());
    }

    @Test
    void verifyRecursiveAndComputeTrustLevelWrongSignature() throws ParseException, JOSEException {
        final UserKeys aliceKeys = UserKeys.create();
        final P384KeyPair bobEcdsaKeys = P384KeyPair.generate();
        final P384KeyPair bobEcdhKeys = P384KeyPair.generate();
        final P384KeyPair oscarEcdsaKeys = P384KeyPair.generate();

        final UserDto alice = new UserDto().id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(aliceKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(aliceKeys.ecdsaKeyPair().getPublic()));
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(bobEcdhKeys.getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobEcdsaKeys.getPublic()));

        final String signature = WoT.sign(oscarEcdsaKeys.getPrivate(), alice.getId(), bob);
        final SecurityFailure e = assertThrows(SecurityFailure.class, () -> WoT.verifyRecursive(Collections.singletonList(signature), aliceKeys.ecdsaKeyPair().getPublic(), SignedKeys.fromUser(bob)));
        assertEquals("Invalid signature", e.getCause().getMessage());

        final TrustedUserDto bobTrust = new TrustedUserDto();
        bobTrust.setTrustedUserId(bob.getId());
        bobTrust.setSignatureChain(Collections.singletonList(signature));

        assertEquals(-1, WoT.computeTrustLevel(bob, bobTrust, alice));
    }

    @Test
    void verifyRecursiveAndComputeTrustLevelValidChain() throws ParseException, JOSEException, SecurityFailure {
        final UserKeys aliceKeys = UserKeys.create();

        final P384KeyPair bobEcdhKeys = P384KeyPair.generate();
        final P384KeyPair bobEcdsaKeys = P384KeyPair.generate();

        final UserDto alice = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("alice")
                .ecdhPublicKey(aliceKeys.encodedEcdhPublicKey())
                .ecdsaPublicKey(aliceKeys.encodedEcdsaPublicKey());
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("bob")
                .ecdhPublicKey(encodePublicKey(bobEcdhKeys.getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobEcdsaKeys.getPublic()));

        final String signature = WoT.sign(aliceKeys.ecdsaKeyPair().getPrivate(), alice.getId(), bob);
        WoT.verifyRecursive(Collections.singletonList(signature), aliceKeys.ecdsaKeyPair().getPublic(), SignedKeys.fromUser(bob));

        final TrustedUserDto bobTrust = new TrustedUserDto();
        bobTrust.setTrustedUserId(bob.getId());
        bobTrust.setSignatureChain(Collections.singletonList(signature));

        assertEquals(1, WoT.computeTrustLevel(bob, bobTrust, alice));
    }

    @Test
    void verifyRecursiveAndComputeTrustLevelNotAllegedSignedKeys() throws ParseException, JOSEException, ApiException, AccessException, SecurityFailure {
        final UserKeys aliceKeys = UserKeys.create();

        final P384KeyPair bobEcdsaKeys = P384KeyPair.generate();
        final P384KeyPair bobEcdhKeys = P384KeyPair.generate();

        final P384KeyPair oscarEcdhKeys = P384KeyPair.generate();
        final P384KeyPair oscarEcdsaKeys = P384KeyPair.generate();

        final UserDto alice = new UserDto().id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(aliceKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(aliceKeys.ecdsaKeyPair().getPublic()));

        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(bobEcdhKeys.getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobEcdsaKeys.getPublic()));
        final UserDto oscar = new UserDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(encodePublicKey(oscarEcdhKeys.getPublic()))
                .ecdsaPublicKey(encodePublicKey(oscarEcdsaKeys.getPublic()));

        final UsersResourceApi usersMock = Mockito.mock(UsersResourceApi.class);
        final UserKeysService userKeysServiceMock = Mockito.mock(UserKeysService.class);
        final Host hub = Mockito.mock(Host.class);
        Mockito.when(usersMock.apiUsersMeGet(true)).thenReturn(alice);
        Mockito.when(userKeysServiceMock.getUserKeys(hub, FirstLoginDeviceSetupCallback.disabled)).thenReturn(aliceKeys);

        final String signature = WoT.sign(aliceKeys.ecdsaKeyPair().getPrivate(), alice.getId(), bob);

        final SecurityFailure e = assertThrows(SecurityFailure.class, () -> WoT.verifyRecursive(Collections.singletonList(signature), aliceKeys.ecdsaKeyPair().getPublic(), SignedKeys.fromUser(oscar)));
        assertTrue(e.getMessage().startsWith("Alleged public key does not match signed public key."));

        final TrustedUserDto bobTrust = new TrustedUserDto();
        bobTrust.setTrustedUserId(bob.getId());
        bobTrust.setSignatureChain(Collections.singletonList(signature));

        assertEquals(-1, WoT.computeTrustLevel(oscar, bobTrust, alice));
    }

    @Test
    public void verifyTrusts() throws ParseException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException, SecurityFailure {
        final List<String> bobSignatureChain = new LinkedList<>();
        int len = 5;

        final UserKeys bobKeys = UserKeys.create();
        final UserDto bob = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("bob")
                .ecdhPublicKey(encodePublicKey(bobKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(bobKeys.ecdsaKeyPair().getPublic()));
        UserDto previousUser = bob;

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
        }
        final UserDto alice = previousUser;

        final UserKeys oscarKeys = UserKeys.create();
        final UserDto oscar = new UserDto()
                .id(UUID.randomUUID().toString())
                .name("oscar")
                .ecdhPublicKey(encodePublicKey(oscarKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(oscarKeys.ecdsaKeyPair().getPublic()));

        // valid signature chain
        final TrustedUserDto bobTrust = new TrustedUserDto().trustedUserId(bob.getId()).signatureChain(bobSignatureChain);
        // invalid signature chain
        final TrustedUserDto oscarTrust = new TrustedUserDto().trustedUserId(oscar.getId()).signatureChain(bobSignatureChain);

        final Map<TrustedUserDto, Integer> actual = WoT.verifyTrusts(Arrays.asList(bobTrust, oscarTrust), Arrays.asList(alice, bob, oscar), decodePublicKey(alice.getEcdsaPublicKey()));
        assertEquals(Collections.singletonMap(bobTrust, len), actual);
    }
}