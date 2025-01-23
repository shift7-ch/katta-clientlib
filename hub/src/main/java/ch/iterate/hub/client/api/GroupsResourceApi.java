package ch.iterate.hub.client.api;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiResponse;
import ch.iterate.hub.client.Configuration;
import ch.iterate.hub.client.Pair;

import javax.ws.rs.core.GenericType;

import ch.iterate.hub.client.model.GroupDto;
import ch.iterate.hub.client.model.UserDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
public class GroupsResourceApi {
  private ApiClient apiClient;

  public GroupsResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public GroupsResourceApi(ApiClient apiClient) {
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
   * list all groups
   * 
   * @return List&lt;GroupDto&gt;
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
  public List<GroupDto> apiGroupsGet() throws ApiException {
    return apiGroupsGetWithHttpInfo().getData();
  }

  /**
   * list all groups
   * 
   * @return ApiResponse&lt;List&lt;GroupDto&gt;&gt;
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
  public ApiResponse<List<GroupDto>> apiGroupsGetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<GroupDto>> localVarReturnType = new GenericType<List<GroupDto>>() {};
    return apiClient.invokeAPI("GroupsResourceApi.apiGroupsGet", "/api/groups", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * list all effective group members
   * 
   * @param groupId  (required)
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
  public List<UserDto> apiGroupsGroupIdEffectiveMembersGet(String groupId) throws ApiException {
    return apiGroupsGroupIdEffectiveMembersGetWithHttpInfo(groupId).getData();
  }

  /**
   * list all effective group members
   * 
   * @param groupId  (required)
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
  public ApiResponse<List<UserDto>> apiGroupsGroupIdEffectiveMembersGetWithHttpInfo(String groupId) throws ApiException {
    // Check required parameters
    if (groupId == null) {
      throw new ApiException(400, "Missing the required parameter 'groupId' when calling apiGroupsGroupIdEffectiveMembersGet");
    }

    // Path parameters
    String localVarPath = "/api/groups/{groupId}/effective-members"
            .replaceAll("\\{groupId}", apiClient.escapeString(groupId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<UserDto>> localVarReturnType = new GenericType<List<UserDto>>() {};
    return apiClient.invokeAPI("GroupsResourceApi.apiGroupsGroupIdEffectiveMembersGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
}
