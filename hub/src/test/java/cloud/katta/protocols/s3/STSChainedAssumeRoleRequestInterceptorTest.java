/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.s3;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.exception.AccessDeniedException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import com.auth0.jwt.JWT;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

class STSChainedAssumeRoleRequestInterceptorTest {

    @Test
    void testMissingClaimAWS() throws Exception {
        final UUID vault = UUID.randomUUID();
        final Host host = Mockito.mock(Host.class);
        when(host.getCredentials()).thenReturn(new Credentials());
        when(host.getProtocol()).thenReturn(new S3AssumeRoleProtocol());
        final STSChainedAssumeRoleRequestInterceptor interceptor = new STSChainedAssumeRoleRequestInterceptor(Mockito.mock(),
                Mockito.mock(), vault,
                host,
                Mockito.mock(),
                Mockito.mock(),
                Mockito.mock());
        assertThrows(AccessDeniedException.class, () -> interceptor.validateVaultClaims(JWT.decode("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzSzQ4SHp3emRXVFh2SWJvN0ZPcjByUDFTUlpZTDQxYWZoSXo3WnNDWDFRIn0.eyJleHAiOjE3NjU1NzI4MDIsImlhdCI6MTc2NTU3MjUwMiwianRpIjoiZnRydHRlOjc0YjVjMzk2LWIxNGQtNGZjNC1mMmE0LTI5OWRlMjNlMWUzMSIsImlzcyI6Imh0dHBzOi8vdGVzdGluZy5rYXR0YS5jbG91ZC9rYy9yZWFsbXMvY2hpcG90bGUiLCJhdWQiOiJjcnlwdG9tYXRvcnZhdWx0cyIsInN1YiI6ImYyOGYxZDhjLTg5ZDYtNDliZS1hNDMwLTZiMGNlZTFkMzlhYyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNyeXB0b21hdG9ydmF1bHRzIiwic2lkIjoiYmFkZmRlOTctYTRhNC0yNzk5LTM3YWYtY2Y5Njg0ZDg1ZTQ5Iiwic2NvcGUiOiI1NDI5YmRmOS0wYjJkLTQ3YTYtOGY3OC1mYzIxMmNiOGNjYjkiLCJjbGllbnRfaWQiOiI1NDI5YmRmOS0wYjJkLTQ3YTYtOGY3OC1mYzIxMmNiOGNjYjkifQ.dQxlf9Nf58pIg9rksNH9CK-GPo59aGFP5RZHJfZ-fa7ortYFtDseIM2JzZF1FV34LDPRz3E1D_RMPfwvEw2VDwyHb3Tcq_x1ZsfIL2ZYJnCbH2pAgao7laF2_Pb8iAJTo15GCAlYFUBTVvyZDVU9paVaBVCOfO0tkeeTtC78RYItUGg1tiyXEUomVaSHUooAYc3tctIgmMWYNZNpFrtfUWaRhrpAtYaxOyqHAfKHCXd21siP-XXF_AfGElUzPkehQxYp1xy5pew1LORO2jyL2Gogn2GjeV0blK6Hb6A2YN-Ul2oxBCC3lXK8E48CuQPTvMx2LYG5zaF575wshkplRQ")));
    }

    @Test
    void testNoClaimForNoAWS() throws Exception {
        final UUID vault = UUID.randomUUID();
        final Host host = Mockito.mock(Host.class);
        when(host.getCredentials()).thenReturn(new Credentials());
        when(host.getProtocol()).thenReturn(new S3AssumeRoleProtocol() {
            @Override
            public String getSTSEndpoint() {
                return "custom";
            }
        });
        final STSChainedAssumeRoleRequestInterceptor interceptor = new STSChainedAssumeRoleRequestInterceptor(Mockito.mock(),
                Mockito.mock(), vault,
                host,
                Mockito.mock(),
                Mockito.mock(),
                Mockito.mock());
        interceptor.validateVaultClaims(JWT.decode("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzSzQ4SHp3emRXVFh2SWJvN0ZPcjByUDFTUlpZTDQxYWZoSXo3WnNDWDFRIn0.eyJleHAiOjE3NjU1NzI4MDIsImlhdCI6MTc2NTU3MjUwMiwianRpIjoiZnRydHRlOjc0YjVjMzk2LWIxNGQtNGZjNC1mMmE0LTI5OWRlMjNlMWUzMSIsImlzcyI6Imh0dHBzOi8vdGVzdGluZy5rYXR0YS5jbG91ZC9rYy9yZWFsbXMvY2hpcG90bGUiLCJhdWQiOiJjcnlwdG9tYXRvcnZhdWx0cyIsInN1YiI6ImYyOGYxZDhjLTg5ZDYtNDliZS1hNDMwLTZiMGNlZTFkMzlhYyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNyeXB0b21hdG9ydmF1bHRzIiwic2lkIjoiYmFkZmRlOTctYTRhNC0yNzk5LTM3YWYtY2Y5Njg0ZDg1ZTQ5Iiwic2NvcGUiOiI1NDI5YmRmOS0wYjJkLTQ3YTYtOGY3OC1mYzIxMmNiOGNjYjkiLCJjbGllbnRfaWQiOiI1NDI5YmRmOS0wYjJkLTQ3YTYtOGY3OC1mYzIxMmNiOGNjYjkifQ.dQxlf9Nf58pIg9rksNH9CK-GPo59aGFP5RZHJfZ-fa7ortYFtDseIM2JzZF1FV34LDPRz3E1D_RMPfwvEw2VDwyHb3Tcq_x1ZsfIL2ZYJnCbH2pAgao7laF2_Pb8iAJTo15GCAlYFUBTVvyZDVU9paVaBVCOfO0tkeeTtC78RYItUGg1tiyXEUomVaSHUooAYc3tctIgmMWYNZNpFrtfUWaRhrpAtYaxOyqHAfKHCXd21siP-XXF_AfGElUzPkehQxYp1xy5pew1LORO2jyL2Gogn2GjeV0blK6Hb6A2YN-Ul2oxBCC3lXK8E48CuQPTvMx2LYG5zaF575wshkplRQ"));
    }

    @Test
    void testNonMatchingVault() throws Exception {
        final UUID vault = UUID.randomUUID();
        final Host host = Mockito.mock(Host.class);
        when(host.getCredentials()).thenReturn(new Credentials());
        when(host.getProtocol()).thenReturn(new S3AssumeRoleProtocol());
        final STSChainedAssumeRoleRequestInterceptor interceptor = new STSChainedAssumeRoleRequestInterceptor(Mockito.mock(),
                Mockito.mock(), vault,
                host,
                Mockito.mock(),
                Mockito.mock(),
                Mockito.mock());
        assertThrows(AccessDeniedException.class, () -> interceptor.validateVaultClaims(JWT.decode("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzSzQ4SHp3emRXVFh2SWJvN0ZPcjByUDFTUlpZTDQxYWZoSXo3WnNDWDFRIn0.eyJleHAiOjE3NjU1NzI0MTUsImlhdCI6MTc2NTU3MjExNSwianRpIjoiZnRydHRlOjYwNWY0YTk2LTNkNTAtZjgyZC01MzVlLTczMDBiYjNjY2MwOSIsImlzcyI6Imh0dHBzOi8vdGVzdGluZy5rYXR0YS5jbG91ZC9rYy9yZWFsbXMvY2hpcG90bGUiLCJhdWQiOiJjcnlwdG9tYXRvcnZhdWx0cyIsInN1YiI6ImYyOGYxZDhjLTg5ZDYtNDliZS1hNDMwLTZiMGNlZTFkMzlhYyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNyeXB0b21hdG9ydmF1bHRzIiwic2lkIjoiOGM4NTQ0OGMtYTBmNi00Y2I4LTMyMjctZWM1YmM4MGRlMWY4Iiwic2NvcGUiOiI0NTBlMTdlZC0xZmI2LTQ5MzAtODU4Ni00MzYwYjc5MGEwNGIiLCJodHRwczovL2F3cy5hbWF6b24uY29tL3RhZ3MiOnsicHJpbmNpcGFsX3RhZ3MiOnsiNDUwZTE3ZWQtMWZiNi00OTMwLTg1ODYtNDM2MGI3OTBhMDRiIjpbIiJdfSwidHJhbnNpdGl2ZV90YWdfa2V5cyI6WyI0NTBlMTdlZC0xZmI2LTQ5MzAtODU4Ni00MzYwYjc5MGEwNGIiXX19.IS7qFXCfKcuqKYvKiO_WOoZjZgXvolG9ifdz426m5G2G7eCCMQE_KfVViRv1ekfuVXh2RJHu3Loowh4KyXpEkY3xOgBdLYnmVWgVBvtU7TqvhHGXsdDIymiJqrcpgmDUqC4YAzqWrCPUHIvkdMcjnjZHvmbY7TF5ehbnUx-RnGlAxomgUrlczLZ_bipMLv37irGElKQv-kv8EQiqQOM-O75E7OPgJsVao7yCBeSKDb6V6BU27v_C5srNUMrqezSyRYapWQJcuNd8Ee_TgLnobEVfDeuj_NLPPlW3p7H0GX4XoDw-PV5O-jfNTtcSBCO8fBPn5IYtcA-Um9ljOGgH7A")));
    }

    @Test
    void testTags() throws Exception {
        final UUID vault = UUID.fromString("450e17ed-1fb6-4930-8586-4360b790a04b");
        final Host host = Mockito.mock(Host.class);
        when(host.getCredentials()).thenReturn(new Credentials());
        when(host.getProtocol()).thenReturn(new S3AssumeRoleProtocol());
        final STSChainedAssumeRoleRequestInterceptor interceptor = new STSChainedAssumeRoleRequestInterceptor(Mockito.mock(),
                Mockito.mock(), vault,
                host,
                Mockito.mock(),
                Mockito.mock(),
                Mockito.mock());
        interceptor.validateVaultClaims(JWT.decode("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzSzQ4SHp3emRXVFh2SWJvN0ZPcjByUDFTUlpZTDQxYWZoSXo3WnNDWDFRIn0.eyJleHAiOjE3NjU1NzI0MTUsImlhdCI6MTc2NTU3MjExNSwianRpIjoiZnRydHRlOjYwNWY0YTk2LTNkNTAtZjgyZC01MzVlLTczMDBiYjNjY2MwOSIsImlzcyI6Imh0dHBzOi8vdGVzdGluZy5rYXR0YS5jbG91ZC9rYy9yZWFsbXMvY2hpcG90bGUiLCJhdWQiOiJjcnlwdG9tYXRvcnZhdWx0cyIsInN1YiI6ImYyOGYxZDhjLTg5ZDYtNDliZS1hNDMwLTZiMGNlZTFkMzlhYyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNyeXB0b21hdG9ydmF1bHRzIiwic2lkIjoiOGM4NTQ0OGMtYTBmNi00Y2I4LTMyMjctZWM1YmM4MGRlMWY4Iiwic2NvcGUiOiI0NTBlMTdlZC0xZmI2LTQ5MzAtODU4Ni00MzYwYjc5MGEwNGIiLCJodHRwczovL2F3cy5hbWF6b24uY29tL3RhZ3MiOnsicHJpbmNpcGFsX3RhZ3MiOnsiNDUwZTE3ZWQtMWZiNi00OTMwLTg1ODYtNDM2MGI3OTBhMDRiIjpbIiJdfSwidHJhbnNpdGl2ZV90YWdfa2V5cyI6WyI0NTBlMTdlZC0xZmI2LTQ5MzAtODU4Ni00MzYwYjc5MGEwNGIiXX19.IS7qFXCfKcuqKYvKiO_WOoZjZgXvolG9ifdz426m5G2G7eCCMQE_KfVViRv1ekfuVXh2RJHu3Loowh4KyXpEkY3xOgBdLYnmVWgVBvtU7TqvhHGXsdDIymiJqrcpgmDUqC4YAzqWrCPUHIvkdMcjnjZHvmbY7TF5ehbnUx-RnGlAxomgUrlczLZ_bipMLv37irGElKQv-kv8EQiqQOM-O75E7OPgJsVao7yCBeSKDb6V6BU27v_C5srNUMrqezSyRYapWQJcuNd8Ee_TgLnobEVfDeuj_NLPPlW3p7H0GX4XoDw-PV5O-jfNTtcSBCO8fBPn5IYtcA-Um9ljOGgH7A"));
    }
}
