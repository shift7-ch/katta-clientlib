package ch.iterate.hub.client;

import ch.cyberduck.core.ConnectionTimeoutFactory;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.PreferencesUseragentProvider;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.jersey.HttpComponentsProvider;

import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.internal.InputStreamProvider;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class HubApiClient extends ApiClient {

    static {
        Logger.getLogger("org.glassfish.jersey.client.ClientExecutorProvidersConfigurator").setLevel(java.util.logging.Level.SEVERE);
    }

    public HubApiClient(final Host host, final CloseableHttpClient client) {
        final Scheme scheme = host.getProtocol().getScheme();
        final int port = host.getPort();
        final String hostname = host.getHostname();
        final String defaultPath = host.getDefaultPath();
        this.setHttpClient(ClientBuilder.newClient(new ClientConfig()
                .register(new InputStreamProvider())
                .register(MultiPartFeature.class)
                .register(new JSON())
                .register(JacksonFeature.class)
                .connectorProvider(new HttpComponentsProvider(client)))
        );
        final int timeout = ConnectionTimeoutFactory.get().getTimeout() * 1000;
        this.setConnectTimeout(timeout);
        this.setReadTimeout(timeout);
        this.setUserAgent(new PreferencesUseragentProvider().get());
        this.setBasePath(new HostUrlProvider().withPath(true).get(scheme, port, null, hostname, defaultPath));
    }

    @Override
    protected Client buildHttpClient() {
        // No need to build default client
        return null;
    }

    @Override
    public <T> ApiResponse<T> invokeAPI(final String operation, final String path, final String method, final List<Pair> queryParams, final Object body,
                                        final Map<String, String> headerParams, final Map<String, String> cookieParams, final Map<String, Object> formParams, final String accept,
                                        final String contentType, final String[] authNames, final GenericType<T> returnType, final boolean isBodyNullable) throws ApiException {
        try {
            return super.invokeAPI(operation, path, method, queryParams, body, headerParams, cookieParams, formParams, accept, contentType, authNames,
                    returnType, isBodyNullable);
        }
        catch(ProcessingException e) {
            throw new ApiException(e);
        }
    }

    @Override
    protected void updateParamsForAuth(final String[] authNames, final List<Pair> queryParams, final Map<String, String> headerParams,
                                       final Map<String, String> cookieParams, final String payload, final String method, final URI uri) {
        // Handled in interceptor
    }


    // Dirty bugfix for https://github.com/OpenAPITools/openapi-generator/issues/10362
    @Override
    public Entity<?> serialize(Object obj, Map<String, Object> formParams, String contentType, boolean isBodyNullable) throws ApiException {
        if(obj instanceof String) {
            return Entity.entity(obj == null ? "" : (String) obj, contentType);
        }
        return super.serialize(obj, formParams, contentType, isBodyNullable);
    }
}
