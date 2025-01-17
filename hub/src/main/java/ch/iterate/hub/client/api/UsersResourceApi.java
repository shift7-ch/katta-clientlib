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
import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class UsersResourceApi {
    private ApiClient apiClient;

    public UsersResourceApi() {
        this(Configuration.getDefaultApiClient());
    }

    public UsersResourceApi(ApiClient apiClient) {
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
     * list all users
     *
     * @return List&lt;UserDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public List<UserDto> apiUsersGet() throws ApiException {
        return apiUsersGetWithHttpInfo().getData();
    }

    /**
     * list all users
     *
     * @return ApiResponse&lt;List&lt;UserDto&gt;&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<List<UserDto>> apiUsersGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/users";

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

        GenericType<List<UserDto>> localVarReturnType = new GenericType<List<UserDto>>() {
        };

        return apiClient.invokeAPI("UsersResourceApi.apiUsersGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
    }

    /**
     * adds/updates user-specific vault keys
     * Stores one or more vaultid-vaultkey-tuples for the currently logged-in user, as defined in the request body ({vault1: token1, vault2: token2, ...}).
     *
     * @param requestBody (optional)
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> all keys stored </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public void apiUsersMeAccessTokensPost(Map<String, String> requestBody) throws ApiException {
        apiUsersMeAccessTokensPostWithHttpInfo(requestBody);
    }

    /**
     * adds/updates user-specific vault keys
     * Stores one or more vaultid-vaultkey-tuples for the currently logged-in user, as defined in the request body ({vault1: token1, vault2: token2, ...}).
     *
     * @param requestBody (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> all keys stored </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<Void> apiUsersMeAccessTokensPostWithHttpInfo(Map<String, String> requestBody) throws ApiException {
        Object localVarPostBody = requestBody;

        // create path and map variables
        String localVarPath = "/api/users/me/access-tokens";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        return apiClient.invokeAPI("UsersResourceApi.apiUsersMeAccessTokensPost", localVarPath, "POST", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, null, false);
    }

    /**
     * get the logged-in user
     *
     * @param withDevices (optional)
     * @return UserDto
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> returns the current user </td><td>  -  </td></tr>
     * <tr><td> 404 </td><td> no user matching the subject of the JWT passed as Bearer Token </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public UserDto apiUsersMeGet(Boolean withDevices) throws ApiException {
        return apiUsersMeGetWithHttpInfo(withDevices).getData();
    }

    /**
     * get the logged-in user
     *
     * @param withDevices (optional)
     * @return ApiResponse&lt;UserDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> returns the current user </td><td>  -  </td></tr>
     * <tr><td> 404 </td><td> no user matching the subject of the JWT passed as Bearer Token </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<UserDto> apiUsersMeGetWithHttpInfo(Boolean withDevices) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/users/me";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        localVarQueryParams.addAll(apiClient.parameterToPairs("", "withDevices", withDevices));


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {

        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        GenericType<UserDto> localVarReturnType = new GenericType<UserDto>() {
        };

        return apiClient.invokeAPI("UsersResourceApi.apiUsersMeGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
    }

    /**
     * update the logged-in user
     *
     * @param userDto (optional)
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 201 </td><td> user created or updated </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public void apiUsersMePut(UserDto userDto) throws ApiException {
        apiUsersMePutWithHttpInfo(userDto);
    }

    /**
     * update the logged-in user
     *
     * @param userDto (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 201 </td><td> user created or updated </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<Void> apiUsersMePutWithHttpInfo(UserDto userDto) throws ApiException {
        Object localVarPostBody = userDto;

        // create path and map variables
        String localVarPath = "/api/users/me";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        return apiClient.invokeAPI("UsersResourceApi.apiUsersMePut", localVarPath, "PUT", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, null, false);
    }

    /**
     * resets the user account
     *
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 204 </td><td> deleted keys, devices and access permissions </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public void apiUsersMeResetPost() throws ApiException {
        apiUsersMeResetPostWithHttpInfo();
    }

    /**
     * resets the user account
     *
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 204 </td><td> deleted keys, devices and access permissions </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<Void> apiUsersMeResetPostWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/users/me/reset";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {

        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        return apiClient.invokeAPI("UsersResourceApi.apiUsersMeResetPost", localVarPath, "POST", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, null, false);
    }

    /**
     * get trusted users
     * returns a list of users trusted by the currently logged-in user
     *
     * @return List&lt;TrustedUserDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public List<TrustedUserDto> apiUsersTrustedGet() throws ApiException {
        return apiUsersTrustedGetWithHttpInfo().getData();
    }

    /**
     * get trusted users
     * returns a list of users trusted by the currently logged-in user
     *
     * @return ApiResponse&lt;List&lt;TrustedUserDto&gt;&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<List<TrustedUserDto>> apiUsersTrustedGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/users/trusted";

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

        GenericType<List<TrustedUserDto>> localVarReturnType = new GenericType<List<TrustedUserDto>>() {
        };

        return apiClient.invokeAPI("UsersResourceApi.apiUsersTrustedGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
    }

    /**
     * get trust detail for given user
     * returns the shortest found signature chain for the given user
     *
     * @param userId (required)
     * @return TrustedUserDto
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 404 </td><td> if no sufficiently short trust chain between the invoking user and the user with the given id has been found </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public TrustedUserDto apiUsersTrustedUserIdGet(String userId) throws ApiException {
        return apiUsersTrustedUserIdGetWithHttpInfo(userId).getData();
    }

    /**
     * get trust detail for given user
     * returns the shortest found signature chain for the given user
     *
     * @param userId (required)
     * @return ApiResponse&lt;TrustedUserDto&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
     * <tr><td> 404 </td><td> if no sufficiently short trust chain between the invoking user and the user with the given id has been found </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<TrustedUserDto> apiUsersTrustedUserIdGetWithHttpInfo(String userId) throws ApiException {
        Object localVarPostBody = null;

        // verify the required parameter 'userId' is set
        if(userId == null) {
            throw new ApiException(400, "Missing the required parameter 'userId' when calling apiUsersTrustedUserIdGet");
        }

        // create path and map variables
        String localVarPath = "/api/users/trusted/{userId}"
                .replaceAll("\\{" + "userId" + "\\}", apiClient.escapeString(userId.toString()));

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

        GenericType<TrustedUserDto> localVarReturnType = new GenericType<TrustedUserDto>() {
        };

        return apiClient.invokeAPI("UsersResourceApi.apiUsersTrustedUserIdGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                localVarAuthNames, localVarReturnType, false);
    }

    /**
     * adds/updates trust
     * Stores a signature for the given user.
     * @param userId  (required)
     * @param body  (required)
     * @throws ApiException if fails to make API call
     * @http.response.details
    <table summary="Response Details" border="1">
    <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
    <tr><td> 204 </td><td> signature stored </td><td>  -  </td></tr>
    <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
    <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
    </table>
     */
    public void apiUsersTrustedUserIdPut(String userId, String body) throws ApiException {
        apiUsersTrustedUserIdPutWithHttpInfo(userId, body);
    }

    /**
     * adds/updates trust
     * Stores a signature for the given user.
     *
     * @param userId (required)
     * @param body   (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 204 </td><td> signature stored </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<Void> apiUsersTrustedUserIdPutWithHttpInfo(String userId, String body) throws ApiException {
        Object localVarPostBody = body;

        // verify the required parameter 'userId' is set
        if(userId == null) {
            throw new ApiException(400, "Missing the required parameter 'userId' when calling apiUsersTrustedUserIdPut");
        }

        // verify the required parameter 'body' is set
        if(body == null) {
            throw new ApiException(400, "Missing the required parameter 'body' when calling apiUsersTrustedUserIdPut");
        }

        // create path and map variables
        String localVarPath = "/api/users/trusted/{userId}"
                .replaceAll("\\{" + "userId" + "\\}", apiClient.escapeString(userId.toString()));

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

        return apiClient.invokeAPI("UsersResourceApi.apiUsersTrustedUserIdPut", localVarPath, "PUT", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
}
