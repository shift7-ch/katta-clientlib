package ch.iterate.hub.client.api;

import javax.ws.rs.core.GenericType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.ApiResponse;
import ch.iterate.hub.client.Configuration;
import ch.iterate.hub.client.Pair;
import ch.iterate.hub.client.model.ConfigDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class ConfigResourceApi {
    private ApiClient apiClient;

    public ConfigResourceApi() {
        this(Configuration.getDefaultApiClient());
    }

    public ConfigResourceApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get the API client
     *
     * @return API client
     */
    public ApiClient getApiClient() {
        return apiClient;
    }

    /**
     * Set the API client
     *
     * @param apiClient an instance of API client
     */
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * @return ConfigDto
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * </table>
     */
    public ConfigDto apiConfigGet() throws ApiException {
        return apiConfigGetWithHttpInfo().getData();
    }

    /**
     * @return ApiResponse&lt;ConfigDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<ConfigDto> apiConfigGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/config";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {

        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        GenericType<ConfigDto> localVarReturnType = new GenericType<ConfigDto>() {};

        return apiClient.invokeAPI("ConfigResourceApi.apiConfigGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
    }
}
