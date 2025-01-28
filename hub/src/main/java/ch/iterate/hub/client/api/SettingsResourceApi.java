package ch.iterate.hub.client.api;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiResponse;
import ch.iterate.hub.client.Configuration;
import ch.iterate.hub.client.Pair;

import javax.ws.rs.core.GenericType;

import ch.iterate.hub.client.model.SettingsDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
public class SettingsResourceApi {
  private ApiClient apiClient;

  public SettingsResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public SettingsResourceApi(ApiClient apiClient) {
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
   * @return SettingsDto
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
  public SettingsDto apiSettingsGet() throws ApiException {
    return apiSettingsGetWithHttpInfo().getData();
  }

  /**
   * get the billing information
   * 
   * @return ApiResponse&lt;SettingsDto&gt;
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
  public ApiResponse<SettingsDto> apiSettingsGetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<SettingsDto> localVarReturnType = new GenericType<SettingsDto>() {};
    return apiClient.invokeAPI("SettingsResourceApi.apiSettingsGet", "/api/settings", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * update settings
   * 
   * @param settingsDto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> token set </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> invalid settings </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> only admins are allowed to update settings </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiSettingsPut(SettingsDto settingsDto) throws ApiException {
    apiSettingsPutWithHttpInfo(settingsDto);
  }

  /**
   * update settings
   * 
   * @param settingsDto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> token set </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> invalid settings </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> only admins are allowed to update settings </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiSettingsPutWithHttpInfo(SettingsDto settingsDto) throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("SettingsResourceApi.apiSettingsPut", "/api/settings", "PUT", new ArrayList<>(), settingsDto,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
}
