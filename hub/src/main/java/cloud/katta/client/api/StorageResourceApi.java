package cloud.katta.client.api;

import cloud.katta.client.ApiException;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiResponse;
import cloud.katta.client.Configuration;
import cloud.katta.client.Pair;

import javax.ws.rs.core.GenericType;

import cloud.katta.client.model.CreateS3STSBucketDto;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.14.0")
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
   * @param createS3STSBucketDto  (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bucket and Keycloak config created </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Could not create bucket </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> Vault with this ID or bucket with this name already exists </td><td>  -  </td></tr>
       <tr><td> 410 </td><td> Storage profile is archived </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     </table>
   */
  public void apiStorageVaultIdPut(@javax.annotation.Nonnull UUID vaultId, @javax.annotation.Nonnull CreateS3STSBucketDto createS3STSBucketDto) throws ApiException {
    apiStorageVaultIdPutWithHttpInfo(vaultId, createS3STSBucketDto);
  }

  /**
   * creates bucket and policy
   * creates an S3 bucket and uploads policy for it.
   * @param vaultId  (required)
   * @param createS3STSBucketDto  (required)
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
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> apiStorageVaultIdPutWithHttpInfo(@javax.annotation.Nonnull UUID vaultId, @javax.annotation.Nonnull CreateS3STSBucketDto createS3STSBucketDto) throws ApiException {
    // Check required parameters
    if (vaultId == null) {
      throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiStorageVaultIdPut");
    }
    if (createS3STSBucketDto == null) {
      throw new ApiException(400, "Missing the required parameter 'createS3STSBucketDto' when calling apiStorageVaultIdPut");
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
