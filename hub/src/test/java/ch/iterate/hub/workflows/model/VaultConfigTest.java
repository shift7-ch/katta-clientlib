/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows.model;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import ch.iterate.hub.client.model.ConfigDto;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JOSEException;
import net.iharder.Base64;

import static ch.iterate.hub.workflows.model.VaultConfig.createVaultConfig;
import static ch.iterate.hub.workflows.model.VaultConfig.rootDirHash;
import static org.junit.jupiter.api.Assertions.assertEquals;


class VaultConfigTest {
    @Test
    public void testCreateVaultConfig() throws JOSEException, IOException {
        final String sharedKey = new String(Base64.decode("oWXcXUeAzfkU1yWNaDEWGZqIsrnUIK4Go7KCtZkGrQXpmILkza+2aB4uhyTCvHGqpS+leX+74opNB2VC39VA4Q=="));
        final String expectedJWT = "eyJraWQiOiJodWIraHR0cDovL2xvY2FsaG9zdDozMDAwL2FwaS92YXVsdHMvNzk3NWFhNDctYzFjZC00Y2VlLWIyMGYtNmQ1NjkwZTIwOWEzIiwidHlwIjoiand0IiwiYWxnIjoiSFMyNTYiLCJodWIiOnsiY2xpZW50SWQiOiJjcnlwdG9tYXRvciIsImF1dGhFbmRwb2ludCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODE4MC9yZWFsbXMvY3J5cHRvbWF0b3IvcHJvdG9jb2wvb3BlbmlkLWNvbm5lY3QvYXV0aCIsInRva2VuRW5kcG9pbnQiOiJodHRwOi8vbG9jYWxob3N0OjgxODAvcmVhbG1zL2NyeXB0b21hdG9yL3Byb3RvY29sL29wZW5pZC1jb25uZWN0L3Rva2VuIiwiZGV2aWNlc1Jlc291cmNlVXJsIjoiaHR0cDovL2xvY2FsaG9zdDozMDAwL2FwaS9kZXZpY2VzLyIsImF1dGhTdWNjZXNzVXJsIjoiaHR0cDovL2xvY2FsaG9zdDozMDAwL2FwcC91bmxvY2stc3VjY2Vzcz92YXVsdD03OTc1YWE0Ny1jMWNkLTRjZWUtYjIwZi02ZDU2OTBlMjA5YTMiLCJhdXRoRXJyb3JVcmwiOiJodHRwOi8vbG9jYWxob3N0OjMwMDAvYXBwL3VubG9jay1lcnJvcj92YXVsdD03OTc1YWE0Ny1jMWNkLTRjZWUtYjIwZi02ZDU2OTBlMjA5YTMifX0.eyJqdGkiOiI3OTc1YWE0Ny1jMWNkLTRjZWUtYjIwZi02ZDU2OTBlMjA5YTMiLCJmb3JtYXQiOjgsImNpcGhlckNvbWJvIjoiU0lWX0dDTSIsInNob3J0ZW5pbmdUaHJlc2hvbGQiOjIyMH0.UCgG17xrumbXlQOUW9NKuyCkrU776JhfmiopbYE9p6k";
        final String vaultId = "7975aa47-c1cd-4cee-b20f-6d5690e209a3";

        final ConfigDto configDto = new ConfigDto()
                .keycloakUrl("http://localhost:3000")
                .keycloakRealm("cryptomator")
                .keycloakClientIdHub("cryptomatorhub")
                .keycloakClientIdCryptomator("cryptomator")
                .keycloakAuthEndpoint("http://localhost:8180/realms/cryptomator/protocol/openid-connect/auth")
                .keycloakTokenEndpoint("http://localhost:8180/realms/cryptomator/protocol/openid-connect/token")
                .serverTime(DateTime.now())
                .apiLevel(0);
        final String absBackendBaseUrl = "http://localhost:3000";
        final String actualJWT = createVaultConfig(vaultId, sharedKey, configDto, absBackendBaseUrl);
        final DecodedJWT actualJWTDecoded = JWT.decode(actualJWT);
        final DecodedJWT expectedJWTDecoded = JWT.decode(expectedJWT);
        assertEquals("{\"clientId\":\"cryptomator\",\"authEndpoint\":\"http://localhost:8180/realms/cryptomator/protocol/openid-connect/auth\",\"tokenEndpoint\":\"http://localhost:8180/realms/cryptomator/protocol/openid-connect/token\",\"devicesResourceUrl\":\"http://localhost:3000/api/devices/\",\"authSuccessUrl\":\"http://localhost:3000/app/unlock-success?vault=7975aa47-c1cd-4cee-b20f-6d5690e209a3\",\"authErrorUrl\":\"http://localhost:3000/app/unlock-error?vault=7975aa47-c1cd-4cee-b20f-6d5690e209a3\"}", actualJWTDecoded.getHeaderClaim("hub").toString());

        assertEquals(expectedJWTDecoded.getClaims().keySet(), actualJWTDecoded.getClaims().keySet());
        for(String k : expectedJWTDecoded.getClaims().keySet()) {
            assertEquals(expectedJWTDecoded.getClaim(k).toString(), actualJWTDecoded.getClaim(k).toString(), k);
        }
        assertEquals(expectedJWTDecoded.getHeaderClaim("kid").toString(), actualJWTDecoded.getHeaderClaim("kid").toString());
        assertEquals(expectedJWTDecoded.getHeaderClaim("hub").toString(), actualJWTDecoded.getHeaderClaim("hub").toString());
        assertEquals(expectedJWTDecoded.getHeaderClaim("typ").toString().toLowerCase(), actualJWTDecoded.getHeaderClaim("typ").toString().toLowerCase());
        assertEquals(expectedJWTDecoded.getHeaderClaim("alg").toString().toLowerCase(), actualJWTDecoded.getHeaderClaim("alg").toString().toLowerCase());
    }

    @Test
    void testRootDirHash() throws IOException {
        assertEquals("TMZSD6SJIM3C6L5P2MGK5X2VMZEQP645", rootDirHash(Base64.decode("oWXcXUeAzfkU1yWNaDEWGZqIsrnUIK4Go7KCtZkGrQXpmILkza+2aB4uhyTCvHGqpS+leX+74opNB2VC39VA4Q==")));
    }

    @Test
    void testRootDirHash2() throws IOException {
        // a024
        assertEquals("HGIBPXCTWSWKKRNZ6TX5I6B7YWG2C2I2", rootDirHash(Base64.decode("JrXxCoCdB/k+c1BvFX0s+OQ2wfgtwsqnkosZquar5D8FxgKXWgOl7JDGcTcPDsx32Lt8IztjpCdG2NDgK+ATtg==")));
    }
}
