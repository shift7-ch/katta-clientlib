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
import ch.iterate.hub.client.model.LicenseUserInfoDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class LicenseResourceApi {
    private ApiClient apiClient;

    public LicenseResourceApi() {
        this(Configuration.getDefaultApiClient());
    }

    public LicenseResourceApi(ApiClient apiClient) {
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
     * Get license information for regular users
     * Information includes the licensed seats, the already used seats and if defined, the license expiration date.
     *
     * @return LicenseUserInfoDto
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public LicenseUserInfoDto apiLicenseUserInfoGet() throws ApiException {
        return apiLicenseUserInfoGetWithHttpInfo().getData();
    }

    /**
     * Get license information for regular users
     * Information includes the licensed seats, the already used seats and if defined, the license expiration date.
     *
     * @return ApiResponse&lt;LicenseUserInfoDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<LicenseUserInfoDto> apiLicenseUserInfoGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/license/user-info";

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

        GenericType<LicenseUserInfoDto> localVarReturnType = new GenericType<LicenseUserInfoDto>() {
        };

        return apiClient.invokeAPI("LicenseResourceApi.apiLicenseUserInfoGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
  }
}
