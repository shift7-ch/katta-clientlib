package ch.iterate.hub.client.api;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiResponse;
import ch.iterate.hub.client.Configuration;
import ch.iterate.hub.client.Pair;

import javax.ws.rs.core.GenericType;

import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
public class StorageProfileResourceApi {
  private ApiClient apiClient;

  public StorageProfileResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public StorageProfileResourceApi(ApiClient apiClient) {
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
   * get configs for storage backends
   * get list of configs for storage backends
   * @param archived  (optional)
   * @return List&lt;StorageProfileDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<StorageProfileDto> apiStorageprofileGet(Boolean archived) throws ApiException {
    return apiStorageprofileGetWithHttpInfo(archived).getData();
  }

  /**
   * get configs for storage backends
   * get list of configs for storage backends
   * @param archived  (optional)
   * @return ApiResponse&lt;List&lt;StorageProfileDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<StorageProfileDto>> apiStorageprofileGetWithHttpInfo(Boolean archived) throws ApiException {
    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "archived", archived)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<StorageProfileDto>> localVarReturnType = new GenericType<List<StorageProfileDto>>() {};
    return apiClient.invokeAPI("StorageProfileResourceApi.apiStorageprofileGet", "/api/storageprofile", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * gets a storage profile
   * 
   * @param profileId  (required)
   * @return StorageProfileDto
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public StorageProfileDto apiStorageprofileProfileIdGet(UUID profileId) throws ApiException {
    return apiStorageprofileProfileIdGetWithHttpInfo(profileId).getData();
  }

  /**
   * gets a storage profile
   * 
   * @param profileId  (required)
   * @return ApiResponse&lt;StorageProfileDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<StorageProfileDto> apiStorageprofileProfileIdGetWithHttpInfo(UUID profileId) throws ApiException {
    // Check required parameters
    if (profileId == null) {
      throw new ApiException(400, "Missing the required parameter 'profileId' when calling apiStorageprofileProfileIdGet");
    }

    // Path parameters
    String localVarPath = "/api/storageprofile/{profileId}"
            .replaceAll("\\{profileId}", apiClient.escapeString(profileId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<StorageProfileDto> localVarReturnType = new GenericType<StorageProfileDto>() {};
    return apiClient.invokeAPI("StorageProfileResourceApi.apiStorageprofileProfileIdGet", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * archive a storage profile
   * 
   * @param profileId  (required)
   * @param archived  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> storage profile archived </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not an admin </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiStorageprofileProfileIdPut(UUID profileId, Boolean archived) throws ApiException {
    apiStorageprofileProfileIdPutWithHttpInfo(profileId, archived);
  }

  /**
   * archive a storage profile
   * 
   * @param profileId  (required)
   * @param archived  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 204 </td><td> storage profile archived </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not an admin </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiStorageprofileProfileIdPutWithHttpInfo(UUID profileId, Boolean archived) throws ApiException {
    // Check required parameters
    if (profileId == null) {
      throw new ApiException(400, "Missing the required parameter 'profileId' when calling apiStorageprofileProfileIdPut");
    }

    // Path parameters
    String localVarPath = "/api/storageprofile/{profileId}"
            .replaceAll("\\{profileId}", apiClient.escapeString(profileId.toString()));

    // Form parameters
    Map<String, Object> localVarFormParams = new LinkedHashMap<>();
    if (archived != null) {
      localVarFormParams.put("archived", archived);
    }

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/x-www-form-urlencoded");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("StorageProfileResourceApi.apiStorageprofileProfileIdPut", localVarPath, "PUT", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * get configs for storage backends
   * get list of configs for storage backends
   * @return List&lt;StorageProfileS3Dto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<StorageProfileS3Dto> apiStorageprofileS3Get() throws ApiException {
    return apiStorageprofileS3GetWithHttpInfo().getData();
  }

  /**
   * get configs for storage backends
   * get list of configs for storage backends
   * @return ApiResponse&lt;List&lt;StorageProfileS3Dto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not a user </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<StorageProfileS3Dto>> apiStorageprofileS3GetWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<StorageProfileS3Dto>> localVarReturnType = new GenericType<List<StorageProfileS3Dto>>() {};
    return apiClient.invokeAPI("StorageProfileResourceApi.apiStorageprofileS3Get", "/api/storageprofile/s3", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * 
   * 
   * @param storageProfileS3Dto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Constraint violation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not an admin </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Storage profile with ID already exists </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiStorageprofileS3Put(StorageProfileS3Dto storageProfileS3Dto) throws ApiException {
    apiStorageprofileS3PutWithHttpInfo(storageProfileS3Dto);
  }

  /**
   * 
   * 
   * @param storageProfileS3Dto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Constraint violation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not an admin </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Storage profile with ID already exists </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiStorageprofileS3PutWithHttpInfo(StorageProfileS3Dto storageProfileS3Dto) throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("StorageProfileResourceApi.apiStorageprofileS3Put", "/api/storageprofile/s3", "PUT", new ArrayList<>(), storageProfileS3Dto,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * 
   * 
   * @param storageProfileS3STSDto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Constraint violation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not an admin </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Storage profile with ID already exists </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiStorageprofileS3stsPut(StorageProfileS3STSDto storageProfileS3STSDto) throws ApiException {
    apiStorageprofileS3stsPutWithHttpInfo(storageProfileS3STSDto);
  }

  /**
   * 
   * 
   * @param storageProfileS3STSDto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> uploaded storage configuration </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Constraint violation </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> not an admin </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Storage profile with ID already exists </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiStorageprofileS3stsPutWithHttpInfo(StorageProfileS3STSDto storageProfileS3STSDto) throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("StorageProfileResourceApi.apiStorageprofileS3stsPut", "/api/storageprofile/s3sts", "PUT", new ArrayList<>(), storageProfileS3STSDto,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
}
