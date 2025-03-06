/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.client.api;

import cloud.katta.client.ApiException;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiResponse;
import cloud.katta.client.Configuration;
import cloud.katta.client.Pair;

import javax.ws.rs.core.GenericType;

import cloud.katta.client.model.TrustedUserDto;
import cloud.katta.client.model.UserDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
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
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<UserDto> apiUsersGet() throws ApiException {
    return apiUsersGetWithHttpInfo().getData();
  }

  /**
   * list all users
   *
   * @return ApiResponse&lt;List&lt;UserDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<UserDto>> apiUsersGetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<UserDto>> localVarReturnType = new GenericType<List<UserDto>>() {};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersGet", "/api/users", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * adds/updates user-specific vault keys
   * Stores one or more vaultid-vaultkey-tuples for the currently logged-in user, as defined in the request body ({vault1: token1, vault2: token2, ...}).
   * @param requestBody  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> all keys stored </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiUsersMeAccessTokensPost(Map<String, String> requestBody) throws ApiException {
    apiUsersMeAccessTokensPostWithHttpInfo(requestBody);
  }

  /**
   * adds/updates user-specific vault keys
   * Stores one or more vaultid-vaultkey-tuples for the currently logged-in user, as defined in the request body ({vault1: token1, vault2: token2, ...}).
   * @param requestBody  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> all keys stored </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiUsersMeAccessTokensPostWithHttpInfo(Map<String, String> requestBody) throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersMeAccessTokensPost", "/api/users/me/access-tokens", "POST", new ArrayList<>(), requestBody,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * get the logged-in user
   *
   * @param withDevices  (optional)
   * @return UserDto
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> returns the current user </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> no user matching the subject of the JWT passed as Bearer Token </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public UserDto apiUsersMeGet(Boolean withDevices) throws ApiException {
    return apiUsersMeGetWithHttpInfo(withDevices).getData();
  }

  /**
   * get the logged-in user
   *
   * @param withDevices  (optional)
   * @return ApiResponse&lt;UserDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> returns the current user </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> no user matching the subject of the JWT passed as Bearer Token </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UserDto> apiUsersMeGetWithHttpInfo(Boolean withDevices) throws ApiException {
    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "withDevices", withDevices)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<UserDto> localVarReturnType = new GenericType<UserDto>() {};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersMeGet", "/api/users/me", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * update the logged-in user
   *
   * @param userDto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 201 </td><td> user created or updated </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiUsersMePut(UserDto userDto) throws ApiException {
    apiUsersMePutWithHttpInfo(userDto);
  }

  /**
   * update the logged-in user
   *
   * @param userDto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 201 </td><td> user created or updated </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiUsersMePutWithHttpInfo(UserDto userDto) throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersMePut", "/api/users/me", "PUT", new ArrayList<>(), userDto,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * resets the user account
   *
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> deleted keys, devices and access permissions </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiUsersMeResetPost() throws ApiException {
    apiUsersMeResetPostWithHttpInfo();
  }

  /**
   * resets the user account
   *
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> deleted keys, devices and access permissions </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiUsersMeResetPostWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersMeResetPost", "/api/users/me/reset", "POST", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * get trusted users
   * returns a list of users trusted by the currently logged-in user
   * @return List&lt;TrustedUserDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<TrustedUserDto> apiUsersTrustedGet() throws ApiException {
    return apiUsersTrustedGetWithHttpInfo().getData();
  }

  /**
   * get trusted users
   * returns a list of users trusted by the currently logged-in user
   * @return ApiResponse&lt;List&lt;TrustedUserDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<TrustedUserDto>> apiUsersTrustedGetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<TrustedUserDto>> localVarReturnType = new GenericType<List<TrustedUserDto>>() {};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersTrustedGet", "/api/users/trusted", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * get trust detail for given user
   * returns the shortest found signature chain for the given user
   * @param userId  (required)
   * @return TrustedUserDto
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> if no sufficiently short trust chain between the invoking user and the user with the given id has been found </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public TrustedUserDto apiUsersTrustedUserIdGet(String userId) throws ApiException {
    return apiUsersTrustedUserIdGetWithHttpInfo(userId).getData();
  }

  /**
   * get trust detail for given user
   * returns the shortest found signature chain for the given user
   * @param userId  (required)
   * @return ApiResponse&lt;TrustedUserDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> if no sufficiently short trust chain between the invoking user and the user with the given id has been found </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TrustedUserDto> apiUsersTrustedUserIdGetWithHttpInfo(String userId) throws ApiException {
    // Check required parameters
    if (userId == null) {
      throw new ApiException(400, "Missing the required parameter 'userId' when calling apiUsersTrustedUserIdGet");
    }

    // Path parameters
    String localVarPath = "/api/users/trusted/{userId}"
            .replaceAll("\\{userId}", apiClient.escapeString(userId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<TrustedUserDto> localVarReturnType = new GenericType<TrustedUserDto>() {};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersTrustedUserIdGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * adds/updates trust
   * Stores a signature for the given user.
   * @param userId  (required)
   * @param body  (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
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
   * @param userId  (required)
   * @param body  (required)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> signature stored </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiUsersTrustedUserIdPutWithHttpInfo(String userId, String body) throws ApiException {
    // Check required parameters
    if (userId == null) {
      throw new ApiException(400, "Missing the required parameter 'userId' when calling apiUsersTrustedUserIdPut");
    }
    if (body == null) {
      throw new ApiException(400, "Missing the required parameter 'body' when calling apiUsersTrustedUserIdPut");
    }

    // Path parameters
    String localVarPath = "/api/users/trusted/{userId}"
            .replaceAll("\\{userId}", apiClient.escapeString(userId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("text/plain");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("UsersResourceApi.apiUsersTrustedUserIdPut", localVarPath, "PUT", new ArrayList<>(), body,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
}
