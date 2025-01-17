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
import ch.iterate.hub.client.model.AuthorityDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class AuthorityResourceApi {
  private ApiClient apiClient;

  public AuthorityResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public AuthorityResourceApi(ApiClient apiClient) {
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
   * lists all authorities matching the given ids
   * lists for each id in the list its corresponding authority. Ignores all id&#39;s where an authority cannot be found
   * @param ids  (optional)
   * @return List&lt;AuthorityDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public List<AuthorityDto> apiAuthoritiesGet(List<String> ids) throws ApiException {
    return apiAuthoritiesGetWithHttpInfo(ids).getData();
  }

  /**
   * lists all authorities matching the given ids
   * lists for each id in the list its corresponding authority. Ignores all id&#39;s where an authority cannot be found
   * @param ids  (optional)
   * @return ApiResponse&lt;List&lt;AuthorityDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<List<AuthorityDto>> apiAuthoritiesGetWithHttpInfo(List<String> ids) throws ApiException {
    Object localVarPostBody = null;

      // create path and map variables
    String localVarPath = "/api/authorities";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    localVarQueryParams.addAll(apiClient.parameterToPairs("multi", "ids", ids));


      final String[] localVarAccepts = {
              "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

      String[] localVarAuthNames = new String[]{};

      GenericType<List<AuthorityDto>> localVarReturnType = new GenericType<List<AuthorityDto>>() {
      };

    return apiClient.invokeAPI("AuthorityResourceApi.apiAuthoritiesGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, localVarReturnType, false);
  }
  /**
   * search authority by name
   * 
   * @param query  (required)
   * @return List&lt;AuthorityDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public List<AuthorityDto> apiAuthoritiesSearchGet(String query) throws ApiException {
    return apiAuthoritiesSearchGetWithHttpInfo(query).getData();
  }

  /**
   * search authority by name
   * 
   * @param query  (required)
   * @return ApiResponse&lt;List&lt;AuthorityDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<List<AuthorityDto>> apiAuthoritiesSearchGetWithHttpInfo(String query) throws ApiException {
    Object localVarPostBody = null;

      // verify the required parameter 'query' is set
      if(query == null) {
      throw new ApiException(400, "Missing the required parameter 'query' when calling apiAuthoritiesSearchGet");
    }

      // create path and map variables
    String localVarPath = "/api/authorities/search";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    localVarQueryParams.addAll(apiClient.parameterToPairs("", "query", query));


      final String[] localVarAccepts = {
              "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

      String[] localVarAuthNames = new String[]{};

      GenericType<List<AuthorityDto>> localVarReturnType = new GenericType<List<AuthorityDto>>() {
      };

    return apiClient.invokeAPI("AuthorityResourceApi.apiAuthoritiesSearchGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, localVarReturnType, false);
  }
}
