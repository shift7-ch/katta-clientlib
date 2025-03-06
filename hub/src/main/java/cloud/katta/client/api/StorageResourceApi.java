/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.client.api;

import cloud.katta.client.ApiException;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiResponse;
import cloud.katta.client.Configuration;

import cloud.katta.client.model.CreateS3STSBucketDto;
import java.util.UUID;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.11.0")
public class StorageResourceApi {
  private ApiClient apiClient;

  public StorageResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public StorageResourceApi(ApiClient apiClient) {
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
   * creates bucket and policy
   * creates an S3 bucket and uploads policy for it.
   * @param vaultId  (required)
   * @param createS3STSBucketDto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bucket and Keycloak config created </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Could not create bucket </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Vault with this ID or bucket with this name already exists </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Storage profile is archived </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public void apiStorageVaultIdPut(UUID vaultId, CreateS3STSBucketDto createS3STSBucketDto) throws ApiException {
    apiStorageVaultIdPutWithHttpInfo(vaultId, createS3STSBucketDto);
  }

  /**
   * creates bucket and policy
   * creates an S3 bucket and uploads policy for it.
   * @param vaultId  (required)
   * @param createS3STSBucketDto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bucket and Keycloak config created </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Could not create bucket </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Vault with this ID or bucket with this name already exists </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Storage profile is archived </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiStorageVaultIdPutWithHttpInfo(UUID vaultId, CreateS3STSBucketDto createS3STSBucketDto) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiStorageVaultIdPut");
    }

    // Path parameters
    String localVarPath = "/api/storage/{vaultId}"
            .replaceAll("\\{vaultId}", apiClient.escapeString(vaultId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    return apiClient.invokeAPI("StorageResourceApi.apiStorageVaultIdPut", localVarPath, "PUT", new ArrayList<>(), createS3STSBucketDto,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
}
