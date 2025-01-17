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
import ch.iterate.hub.client.model.BillingDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class BillingResourceApi {
    private ApiClient apiClient;

    public BillingResourceApi() {
        this(Configuration.getDefaultApiClient());
    }

    public BillingResourceApi(ApiClient apiClient) {
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
     * get the billing information
     *
     * @return BillingDto
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> only admins are allowed to get the billing information </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public BillingDto apiBillingGet() throws ApiException {
        return apiBillingGetWithHttpInfo().getData();
    }

    /**
     * get the billing information
     *
     * @return ApiResponse&lt;BillingDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> only admins are allowed to get the billing information </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<BillingDto> apiBillingGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/billing";

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

        GenericType<BillingDto> localVarReturnType = new GenericType<BillingDto>() {
        };

        return apiClient.invokeAPI("BillingResourceApi.apiBillingGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
    }

    /**
     * set the token
     *
     * @param body (required)
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 204 </td><td> token set </td><td>  -  </td></tr>
     * <tr><td> 400 </td><td> token is invalid (e.g., expired or invalid signature) </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> only admins are allowed to set the token </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public void apiBillingTokenPut(String body) throws ApiException {
        apiBillingTokenPutWithHttpInfo(body);
    }

    /**
     * set the token
     *
     * @param body (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 204 </td><td> token set </td><td>  -  </td></tr>
     * <tr><td> 400 </td><td> token is invalid (e.g., expired or invalid signature) </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> only admins are allowed to set the token </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<Void> apiBillingTokenPutWithHttpInfo(String body) throws ApiException {
        Object localVarPostBody = body;

        // verify the required parameter 'body' is set
        if(body == null) {
            throw new ApiException(400, "Missing the required parameter 'body' when calling apiBillingTokenPut");
        }

        // create path and map variables
        String localVarPath = "/api/billing/token";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "text/plain"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        return apiClient.invokeAPI("BillingResourceApi.apiBillingTokenPut", localVarPath, "PUT", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, null, false);
  }
}
