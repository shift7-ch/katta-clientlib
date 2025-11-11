/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.cli.commands.storage;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.ListOpenIdConnectProvidersResponse;
import software.amazon.awssdk.services.iam.model.OpenIDConnectProviderListEntry;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.UpdateAssumeRolePolicyRequest;

public class AwsStsSetupTest {
    @Test
    public void testAwsSetup() throws IOException, InterruptedException {
        final IamClient iam = Mockito.mock(IamClient.class);
        Mockito.when(iam.listOpenIDConnectProviders()).thenReturn(ListOpenIdConnectProvidersResponse.builder().openIDConnectProviderList(OpenIDConnectProviderListEntry.builder().arn("arnP").build()).build());
        Mockito.when(iam.getRole(GetRoleRequest.builder().roleName("prae-access-bucket-a-role-web-identity").build())).thenReturn(GetRoleResponse.builder().role(Role.builder().arn("arn").build()).build());
        final AwsStsSetup cli = new AwsStsSetup();
        cli.bucketPrefix = "prefix-";
        cli.roleNamePrefix = "prae-";
        cli.millis = 1;
        cli.call(iam, "arnP", "alskdjfkl");
        // 3 = 1 x create-bucket and 2 x access-bucket
        Mockito.verify(iam, Mockito.times(3)).putRolePolicy(Mockito.any(PutRolePolicyRequest.class));
        Mockito.verify(iam, Mockito.times(3)).updateAssumeRolePolicy(Mockito.any(UpdateAssumeRolePolicyRequest.class));
    }
}
