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

import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.Role;
import java.util.UUID;
import cloud.katta.client.model.VaultDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
public class VaultResourceApi {
  private ApiClient apiClient;

  public VaultResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public VaultResourceApi(ApiClient apiClient) {
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
   * list all accessible vaults
   * list all vaults that have been shared with the currently logged in user or a group in wich this user is
   * @param role  (optional)
   * @return List&lt;VaultDto&gt;
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
  public List<VaultDto> apiVaultsAccessibleGet(Role role) throws ApiException {
    return apiVaultsAccessibleGetWithHttpInfo(role).getData();
  }

  /**
   * list all accessible vaults
   * list all vaults that have been shared with the currently logged in user or a group in wich this user is
   * @param role  (optional)
   * @return ApiResponse&lt;List&lt;VaultDto&gt;&gt;
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
  public ApiResponse<List<VaultDto>> apiVaultsAccessibleGetWithHttpInfo(Role role) throws ApiException {
    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "role", role)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<VaultDto>> localVarReturnType = new GenericType<List<VaultDto>>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsAccessibleGet", "/api/vaults/accessible", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * list all vaults
   * list all vaults in the system
   * @return List&lt;VaultDto&gt;
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
  public List<VaultDto> apiVaultsAllGet() throws ApiException {
    return apiVaultsAllGetWithHttpInfo().getData();
  }

  /**
   * list all vaults
   * list all vaults in the system
   * @return ApiResponse&lt;List&lt;VaultDto&gt;&gt;
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
  public ApiResponse<List<VaultDto>> apiVaultsAllGetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<VaultDto>> localVarReturnType = new GenericType<List<VaultDto>>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsAllGet", "/api/vaults/all", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * list all vaults corresponding to the given ids
   * list for each id in the list its corresponding vault. Ignores all id&#39;s where a vault does not exist,
   * @param ids  (optional)
   * @return List&lt;VaultDto&gt;
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
  public List<VaultDto> apiVaultsSomeGet(List<UUID> ids) throws ApiException {
    return apiVaultsSomeGetWithHttpInfo(ids).getData();
  }

  /**
   * list all vaults corresponding to the given ids
   * list for each id in the list its corresponding vault. Ignores all id&#39;s where a vault does not exist,
   * @param ids  (optional)
   * @return ApiResponse&lt;List&lt;VaultDto&gt;&gt;
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
  public ApiResponse<List<VaultDto>> apiVaultsSomeGetWithHttpInfo(List<UUID> ids) throws ApiException {
    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("multi", "ids", ids)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<VaultDto>> localVarReturnType = new GenericType<List<VaultDto>>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsSomeGet", "/api/vaults/some", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * get the user-specific vault key
   * retrieves a jwe containing the vault key, encrypted for the current user
   * @param vaultId  (required)
   * @param evenIfArchived  (optional, default to false)
   * @return String
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> license expired or number of effective vault users that have a token exceeds available license seats </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault member </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> unknown vault </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Vault is archived. Only returned if evenIfArchived query param is false or not set, otherwise the archived flag is ignored </td><td>  -  </td></tr>
       <tr><td> 449 </td><td> User account not yet initialized. Retry after setting up user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public String apiVaultsVaultIdAccessTokenGet(UUID vaultId, Boolean evenIfArchived) throws ApiException {
    return apiVaultsVaultIdAccessTokenGetWithHttpInfo(vaultId, evenIfArchived).getData();
  }

  /**
   * get the user-specific vault key
   * retrieves a jwe containing the vault key, encrypted for the current user
   * @param vaultId  (required)
   * @param evenIfArchived  (optional, default to false)
   * @return ApiResponse&lt;String&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> license expired or number of effective vault users that have a token exceeds available license seats </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault member </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> unknown vault </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Vault is archived. Only returned if evenIfArchived query param is false or not set, otherwise the archived flag is ignored </td><td>  -  </td></tr>
       <tr><td> 449 </td><td> User account not yet initialized. Retry after setting up user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<String> apiVaultsVaultIdAccessTokenGetWithHttpInfo(UUID vaultId, Boolean evenIfArchived) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdAccessTokenGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/access-token"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "evenIfArchived", evenIfArchived)
    );

    String localVarAccept = apiClient.selectHeaderAccept("text/plain");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<String> localVarReturnType = new GenericType<String>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdAccessTokenGet", localVarPath, "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * adds user-specific vault keys
   * Stores one or more user-vaultkey-tuples, as defined in the request body ({user1: token1, user2: token2, ...}).
   * @param vaultId  (required)
   * @param requestBody  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> all keys stored </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> number of users granted access exceeds available license seats </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> at least one user has not been found </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiVaultsVaultIdAccessTokensPost(UUID vaultId, Map<String, String> requestBody) throws ApiException {
    apiVaultsVaultIdAccessTokensPostWithHttpInfo(vaultId, requestBody);
  }

  /**
   * adds user-specific vault keys
   * Stores one or more user-vaultkey-tuples, as defined in the request body ({user1: token1, user2: token2, ...}).
   * @param vaultId  (required)
   * @param requestBody  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> all keys stored </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> number of users granted access exceeds available license seats </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> at least one user has not been found </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiVaultsVaultIdAccessTokensPostWithHttpInfo(UUID vaultId, Map<String, String> requestBody) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdAccessTokensPost");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/access-tokens"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdAccessTokensPost", localVarPath, "POST", new ArrayList<>(), requestBody,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * remove a user or group from this vault
   * revokes the given authority&#39;s access rights from this vault. If the given authority is no member, the request is a no-op.
   * @param authorityId  (required)
   * @param vaultId  (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> authority removed </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiVaultsVaultIdAuthorityAuthorityIdDelete(String authorityId, UUID vaultId) throws ApiException {
    apiVaultsVaultIdAuthorityAuthorityIdDeleteWithHttpInfo(authorityId, vaultId);
  }

  /**
   * remove a user or group from this vault
   * revokes the given authority&#39;s access rights from this vault. If the given authority is no member, the request is a no-op.
   * @param authorityId  (required)
   * @param vaultId  (required)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> authority removed </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiVaultsVaultIdAuthorityAuthorityIdDeleteWithHttpInfo(String authorityId, UUID vaultId) throws ApiException {
    // Check required parameters
    if (authorityId == null) {
      throw new ApiException(400, "Missing the required parameter 'authorityId' when calling apiVaultsVaultIdAuthorityAuthorityIdDelete");
    }
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdAuthorityAuthorityIdDelete");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/authority/{authorityId}"
            .replaceAll("\\{authorityId}", apiClient.escapeString(authorityId.toString()))
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdAuthorityAuthorityIdDelete", localVarPath, "DELETE", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * claims ownership of a vault
   * Assigns the OWNER role to the currently logged in user, who proofs this claim by sending a JWT signed with a private key held by users knowing the Vault Admin Password
   * @param vaultId  (required)
   * @param proof  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> ownership claimed successfully </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> incorrect proof </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> no such vault </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> owned by another user </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiVaultsVaultIdClaimOwnershipPost(UUID vaultId, String proof) throws ApiException {
    apiVaultsVaultIdClaimOwnershipPostWithHttpInfo(vaultId, proof);
  }

  /**
   * claims ownership of a vault
   * Assigns the OWNER role to the currently logged in user, who proofs this claim by sending a JWT signed with a private key held by users knowing the Vault Admin Password
   * @param vaultId  (required)
   * @param proof  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> ownership claimed successfully </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> incorrect proof </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> no such vault </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> owned by another user </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiVaultsVaultIdClaimOwnershipPostWithHttpInfo(UUID vaultId, String proof) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdClaimOwnershipPost");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/claim-ownership"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    // Form parameters
    Map<String, Object> localVarFormParams = new LinkedHashMap<>();
    if (proof != null) {
      localVarFormParams.put("proof", proof);
    }

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/x-www-form-urlencoded");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdClaimOwnershipPost", localVarPath, "POST", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * gets a vault
   *
   * @param vaultId  (required)
   * @return VaultDto
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> requesting user is neither a vault member nor has the admin role </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public VaultDto apiVaultsVaultIdGet(UUID vaultId) throws ApiException {
    return apiVaultsVaultIdGetWithHttpInfo(vaultId).getData();
  }

  /**
   * gets a vault
   *
   * @param vaultId  (required)
   * @return ApiResponse&lt;VaultDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> requesting user is neither a vault member nor has the admin role </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<VaultDto> apiVaultsVaultIdGetWithHttpInfo(UUID vaultId) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<VaultDto> localVarReturnType = new GenericType<VaultDto>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * adds a group to this vault or updates its role
   *
   * @param groupId  (required)
   * @param vaultId  (required)
   * @param role the role to grant to this group (defaults to MEMBER) (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> group&#39;s role updated </td><td>  -  </td></tr>
       <tr><td> 201 </td><td> group added </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> license is expired or licensed seats would be exceeded after the operation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> group not found </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiVaultsVaultIdGroupsGroupIdPut(String groupId, UUID vaultId, Role role) throws ApiException {
    apiVaultsVaultIdGroupsGroupIdPutWithHttpInfo(groupId, vaultId, role);
  }

  /**
   * adds a group to this vault or updates its role
   *
   * @param groupId  (required)
   * @param vaultId  (required)
   * @param role the role to grant to this group (defaults to MEMBER) (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> group&#39;s role updated </td><td>  -  </td></tr>
       <tr><td> 201 </td><td> group added </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> license is expired or licensed seats would be exceeded after the operation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> group not found </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiVaultsVaultIdGroupsGroupIdPutWithHttpInfo(String groupId, UUID vaultId, Role role) throws ApiException {
    // Check required parameters
    if (groupId == null) {
      throw new ApiException(400, "Missing the required parameter 'groupId' when calling apiVaultsVaultIdGroupsGroupIdPut");
    }
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdGroupsGroupIdPut");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/groups/{groupId}"
            .replaceAll("\\{groupId}", apiClient.escapeString(groupId.toString()))
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "role", role)
    );

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdGroupsGroupIdPut", localVarPath, "PUT", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * get the device-specific masterkey of a non-archived vault
   *
   * @param deviceId  (required)
   * @param vaultId  (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> number of effective vault users exceeds available license seats </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not authorized to access this vault </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Vault is archived </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   * @deprecated
   */
  @Deprecated
  public void apiVaultsVaultIdKeysDeviceIdGet(String deviceId, UUID vaultId) throws ApiException {
    apiVaultsVaultIdKeysDeviceIdGetWithHttpInfo(deviceId, vaultId);
  }

  /**
   * get the device-specific masterkey of a non-archived vault
   *
   * @param deviceId  (required)
   * @param vaultId  (required)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> number of effective vault users exceeds available license seats </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not authorized to access this vault </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Vault is archived </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   * @deprecated
   */
  @Deprecated
  public ApiResponse<Void> apiVaultsVaultIdKeysDeviceIdGetWithHttpInfo(String deviceId, UUID vaultId) throws ApiException {
    // Check required parameters
    if (deviceId == null) {
      throw new ApiException(400, "Missing the required parameter 'deviceId' when calling apiVaultsVaultIdKeysDeviceIdGet");
    }
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdKeysDeviceIdGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/keys/{deviceId}"
            .replaceAll("\\{deviceId}", apiClient.escapeString(deviceId.toString()))
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdKeysDeviceIdGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * list vault members
   * list all users or groups that this vault has been shared with directly (not inherited via group membership)
   * @param vaultId  (required)
   * @return List&lt;MemberDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<MemberDto> apiVaultsVaultIdMembersGet(UUID vaultId) throws ApiException {
    return apiVaultsVaultIdMembersGetWithHttpInfo(vaultId).getData();
  }

  /**
   * list vault members
   * list all users or groups that this vault has been shared with directly (not inherited via group membership)
   * @param vaultId  (required)
   * @return ApiResponse&lt;List&lt;MemberDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<MemberDto>> apiVaultsVaultIdMembersGetWithHttpInfo(UUID vaultId) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdMembersGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/members"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<MemberDto>> localVarReturnType = new GenericType<List<MemberDto>>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdMembersGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * creates or updates a vault
   * Creates or updates a vault with the given vault id. The creationTime in the vaultDto is always ignored. On creation, the current server time is used and the archived field is ignored. On update, only the name, description, and archived fields are considered.
   * @param vaultId  (required)
   * @param vaultDto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> existing vault updated </td><td>  -  </td></tr>
       <tr><td> 201 </td><td> new vault created </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> number of licensed seats is exceeded </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiVaultsVaultIdPut(UUID vaultId, VaultDto vaultDto) throws ApiException {
    apiVaultsVaultIdPutWithHttpInfo(vaultId, vaultDto);
  }

  /**
   * creates or updates a vault
   * Creates or updates a vault with the given vault id. The creationTime in the vaultDto is always ignored. On creation, the current server time is used and the archived field is ignored. On update, only the name, description, and archived fields are considered.
   * @param vaultId  (required)
   * @param vaultDto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> existing vault updated </td><td>  -  </td></tr>
       <tr><td> 201 </td><td> new vault created </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> number of licensed seats is exceeded </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiVaultsVaultIdPutWithHttpInfo(UUID vaultId, VaultDto vaultDto) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdPut");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdPut", localVarPath, "PUT", new ArrayList<>(), vaultDto,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * list members requiring access tokens
   * lists all members, that have permissions but lack an access token
   * @param vaultId  (required)
   * @return List&lt;MemberDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<MemberDto> apiVaultsVaultIdUsersRequiringAccessGrantGet(UUID vaultId) throws ApiException {
    return apiVaultsVaultIdUsersRequiringAccessGrantGetWithHttpInfo(vaultId).getData();
  }

  /**
   * list members requiring access tokens
   * lists all members, that have permissions but lack an access token
   * @param vaultId  (required)
   * @return ApiResponse&lt;List&lt;MemberDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<MemberDto>> apiVaultsVaultIdUsersRequiringAccessGrantGetWithHttpInfo(UUID vaultId) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdUsersRequiringAccessGrantGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/users-requiring-access-grant"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<MemberDto>> localVarReturnType = new GenericType<List<MemberDto>>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdUsersRequiringAccessGrantGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * adds a user to this vault or updates her role
   *
   * @param userId  (required)
   * @param vaultId  (required)
   * @param role the role to grant to this user (defaults to MEMBER) (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> user&#39;s role updated </td><td>  -  </td></tr>
       <tr><td> 201 </td><td> user added </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> license is expired or licensed seats would be exceeded after the operation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> user not found </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiVaultsVaultIdUsersUserIdPut(String userId, UUID vaultId, Role role) throws ApiException {
    apiVaultsVaultIdUsersUserIdPutWithHttpInfo(userId, vaultId, role);
  }

  /**
   * adds a user to this vault or updates her role
   *
   * @param userId  (required)
   * @param vaultId  (required)
   * @param role the role to grant to this user (defaults to MEMBER) (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> user&#39;s role updated </td><td>  -  </td></tr>
       <tr><td> 201 </td><td> user added </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> license is expired or licensed seats would be exceeded after the operation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a vault owner </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> user not found </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiVaultsVaultIdUsersUserIdPutWithHttpInfo(String userId, UUID vaultId, Role role) throws ApiException {
    // Check required parameters
    if (userId == null) {
      throw new ApiException(400, "Missing the required parameter 'userId' when calling apiVaultsVaultIdUsersUserIdPut");
    }
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdUsersUserIdPut");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/users/{userId}"
            .replaceAll("\\{userId}", apiClient.escapeString(userId.toString()))
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "role", role)
    );

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdUsersUserIdPut", localVarPath, "PUT", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * get public vault keys
   * retrieves a JWK Set containing public keys related to this vault
   * @param vaultId  (required)
   * @return String
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> unknown vault </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public String apiVaultsVaultIdUvfJwksJsonGet(UUID vaultId) throws ApiException {
    return apiVaultsVaultIdUvfJwksJsonGetWithHttpInfo(vaultId).getData();
  }

  /**
   * get public vault keys
   * retrieves a JWK Set containing public keys related to this vault
   * @param vaultId  (required)
   * @return ApiResponse&lt;String&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> unknown vault </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<String> apiVaultsVaultIdUvfJwksJsonGetWithHttpInfo(UUID vaultId) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdUvfJwksJsonGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/uvf/jwks.json"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<String> localVarReturnType = new GenericType<String>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdUvfJwksJsonGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * get the vault.uvf file
   *
   * @param vaultId  (required)
   * @return String
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> unknown vault </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public String apiVaultsVaultIdUvfVaultUvfGet(UUID vaultId) throws ApiException {
    return apiVaultsVaultIdUvfVaultUvfGetWithHttpInfo(vaultId).getData();
  }

  /**
   * get the vault.uvf file
   *
   * @param vaultId  (required)
   * @return ApiResponse&lt;String&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> unknown vault </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<String> apiVaultsVaultIdUvfVaultUvfGetWithHttpInfo(UUID vaultId) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiVaultsVaultIdUvfVaultUvfGet");
    }

    // Path parameters
    String localVarPath = "/api/vaults/{vaultId}/uvf/vault.uvf"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<String> localVarReturnType = new GenericType<String>() {};
    return apiClient.invokeAPI("VaultResourceApi.apiVaultsVaultIdUvfVaultUvfGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
}
