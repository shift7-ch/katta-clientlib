package cloud.katta.client.api;

import cloud.katta.client.ApiException;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiResponse;
import cloud.katta.client.Configuration;
import cloud.katta.client.Pair;

import javax.ws.rs.core.GenericType;

import cloud.katta.client.model.LicenseUserInfoDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.14.0")
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
   * @return LicenseUserInfoDto
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     </table>
   */
  public LicenseUserInfoDto apiLicenseUserInfoGet() throws ApiException {
    return apiLicenseUserInfoGetWithHttpInfo().getData();
  }

  /**
   * Get license information for regular users
   * Information includes the licensed seats, the already used seats and if defined, the license expiration date.
   * @return ApiResponse&lt;LicenseUserInfoDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<LicenseUserInfoDto> apiLicenseUserInfoGetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<LicenseUserInfoDto> localVarReturnType = new GenericType<LicenseUserInfoDto>() {};
    return apiClient.invokeAPI("LicenseResourceApi.apiLicenseUserInfoGet", "/api/license/user-info", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
}
