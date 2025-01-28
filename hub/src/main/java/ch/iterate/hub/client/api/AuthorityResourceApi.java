package ch.iterate.hub.client.api;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiResponse;
import ch.iterate.hub.client.Configuration;
import ch.iterate.hub.client.Pair;

import javax.ws.rs.core.GenericType;

import ch.iterate.hub.client.model.AuthorityDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
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
     <table border="1">
       <caption>Response Details</caption>
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
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<AuthorityDto>> apiAuthoritiesGetWithHttpInfo(List<String> ids) throws ApiException {
    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("multi", "ids", ids)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<AuthorityDto>> localVarReturnType = new GenericType<List<AuthorityDto>>() {};
    return apiClient.invokeAPI("AuthorityResourceApi.apiAuthoritiesGet", "/api/authorities", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * search authority by name
   * 
   * @param query  (required)
   * @return List&lt;AuthorityDto&gt;
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
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<AuthorityDto>> apiAuthoritiesSearchGetWithHttpInfo(String query) throws ApiException {
    // Check required parameters
    if (query == null) {
      throw new ApiException(400, "Missing the required parameter 'query' when calling apiAuthoritiesSearchGet");
    }

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "query", query)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<AuthorityDto>> localVarReturnType = new GenericType<List<AuthorityDto>>() {};
    return apiClient.invokeAPI("AuthorityResourceApi.apiAuthoritiesSearchGet", "/api/authorities/search", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
}
