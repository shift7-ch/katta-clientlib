/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PreferencesUseragentProvider;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Bulk;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.http.UserAgentHttpRequestInitializer;
import ch.cyberduck.core.io.StatusOutputStream;
import ch.cyberduck.core.oauth.OAuth2AuthorizationService;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.Pair;
import cloud.katta.client.api.ConfigResourceApi;
import cloud.katta.client.model.ConfigDto;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.PasswordTokenRequest;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class HubTestUtilities {

    public static ApiClient getAdminApiClient(final HubTestConfig.Setup setup) throws IOException, ApiException {
        final ConfigDto config = new ConfigResourceApi(new ApiClient().setBasePath(setup.hubURL)).apiConfigGet();
        final PasswordTokenRequest request = new PasswordTokenRequest(new ApacheHttpTransport(), new GsonFactory(), new GenericUrl(config.getKeycloakTokenEndpoint()),
                setup.adminConfig.username, setup.adminConfig.password)
                .setClientAuthentication(new ClientParametersAuthentication(setup.clientId, null))
                .setRequestInitializer(new UserAgentHttpRequestInitializer(new PreferencesUseragentProvider()));
        final String adminAccessToken = request.executeUnparsed().parseAs(OAuth2AuthorizationService.PermissiveTokenResponse.class).toTokenResponse().getAccessToken();
        final ApiClient adminApiClient = new ApiClient() {
            @Override
            protected void updateParamsForAuth(final String[] authNames, final List<Pair> queryParams, final Map<String, String> headerParams, final Map<String, String> cookieParams, final String payload, final String method, final URI uri) throws ApiException {
                super.updateParamsForAuth(authNames, queryParams, headerParams, cookieParams, payload, method, uri);
                headerParams.put("Authorization", String.format("Bearer %s", adminAccessToken));
            }
        };
        return adminApiClient.setBasePath(setup.hubURL);
    }

    public static byte[] write(final Session<?> session, final Path file, final byte[] content) throws BackgroundException, IOException {
        final TransferStatus status = new TransferStatus()
                .setLength(content.length);
        status.setChecksum(session.getFeature(Write.class).checksum(file, status).compute(new ByteArrayInputStream(content), status));
        session.getFeature(Bulk.class).pre(Transfer.Type.upload, Collections.singletonMap(new TransferItem(file), status), new DisabledConnectionCallback());
        try (final StatusOutputStream<?> out = session.getFeature(Write.class).write(file, status, new DisabledConnectionCallback())) {
            IOUtils.copyLarge(new ByteArrayInputStream(content), out);
        }
        return content;
    }

    public static byte[] read(final Session<?> session, final Path file, final int length) throws BackgroundException, IOException {
        final TransferStatus status = new TransferStatus()
                .setRemote(file.attributes())
                .setLength(length);
        session.getFeature(Bulk.class).pre(Transfer.Type.download, Collections.singletonMap(new TransferItem(file), status), new DisabledConnectionCallback());
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(length);
        try (final InputStream in = session.getFeature(Read.class).read(file, status, new DisabledConnectionCallback())) {
            IOUtils.copyLarge(in, buffer);
        }
        return buffer.toByteArray();
    }
}
