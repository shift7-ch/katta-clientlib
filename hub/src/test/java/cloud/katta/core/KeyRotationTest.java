/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfAccessTokenPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestSetupDockerExtension;
import cloud.katta.workflows.DeviceKeysServiceImpl;
import cloud.katta.workflows.UserKeysService;
import cloud.katta.workflows.UserKeysServiceImpl;
import cloud.katta.workflows.VaultServiceImpl;

import static cloud.katta.crypto.KeyHelper.decodePublicKey;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({HubTestSetupDockerExtension.Local.class})
public class KeyRotationTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(KeyRotationTest.class.getName());

    private Stream<Arguments> arguments() {
        return Stream.of(LOCAL_MINIO_STS);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void keyrotationTest(final HubTestConfig hubTestConfig) throws Exception {
        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        try {
            final UsersResourceApi usersResourceApi = new UsersResourceApi(hubSession.getClient());
            final VaultResourceApi vaultResourceApi = new VaultResourceApi(hubSession.getClient());
            final UserDto me = usersResourceApi.apiUsersMeGet(false);
            log.info(me);

            final List<VaultDto> vaults = vaultResourceApi.apiVaultsAccessibleGet(Role.OWNER);

            // open to all users - does not require admin privileges in Cryptomator hub!
            final List<UserDto> userDtos = usersResourceApi.apiUsersGet();

            final Map<String, String> userPublicKeys = userDtos.stream()
                    .filter(u -> u.getEcdhPublicKey() != null)
                    .collect(Collectors.toMap(UserDto::getId, UserDto::getEcdhPublicKey));

            for(final VaultDto vaultDto : vaults) {
                final HashMap<String, String> tokens = new HashMap<>();
                final UserKeysService service = new UserKeysServiceImpl(hubSession);
                final UserKeys userKeys = service.getUserKeys(hubSession.getHost(), me, new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost()));
            final VaultServiceImpl vaultService = new VaultServiceImpl(hubSession);
            final UvfMetadataPayload metadataJWE = vaultService.getVaultMetadataJWE(UUID.fromString(vaultDto.getId().toString()), userKeys);
                final UvfAccessTokenPayload masterkeyJWE = vaultService.getVaultAccessTokenJWE(UUID.fromString(vaultDto.getId().toString()), userKeys);

                // TODO https://github.com/shift7-ch/cipherduck-hub/issues/37 change nickname for now - could be used to rotate of shared access key/secret key.
                metadataJWE.storage().nickname(String.format("ZZZZ %s", vaultDto.getName()));
                final List<MemberDto> members = vaultResourceApi.apiVaultsVaultIdMembersGet(vaultDto.getId());
                for(final MemberDto member : members) {
                    if(userPublicKeys.containsKey(member.getId())) {
                        tokens.put(member.getId(), masterkeyJWE.encryptForUser(decodePublicKey(userPublicKeys.get(member.getId()))));
                    }
                }
                vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultDto.getId(), tokens);
            }
        }
        finally {
            hubSession.close();
        }
    }
}
