package ch.iterate.hub.client.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.ApiResponse;
import ch.iterate.hub.client.Configuration;
import ch.iterate.hub.client.Pair;
import ch.iterate.hub.client.model.CreateS3STSBucketDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
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
     *
     * @param vaultId              (required)
     * @param createS3STSBucketDto (optional)
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> Bucket and Keycloak config created </td><td>  -  </td></tr>
     * <tr><td> 400 </td><td> Could not create bucket </td><td>  -  </td></tr>
     * <tr><td> 409 </td><td> Vault with this ID or bucket with this name already exists </td><td>  -  </td></tr>
     * <tr><td> 410 </td><td> Storage profile is archived </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public void apiStorageVaultIdPut(UUID vaultId, CreateS3STSBucketDto createS3STSBucketDto) throws ApiException {
        apiStorageVaultIdPutWithHttpInfo(vaultId, createS3STSBucketDto);
    }

    /**
     * creates bucket and policy
     * creates an S3 bucket and uploads policy for it.
     *
     * @param vaultId              (required)
     * @param createS3STSBucketDto (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException if fails to make API call
     * @http.response.details <table summary="Response Details" border="1">
     * <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
     * <tr><td> 200 </td><td> Bucket and Keycloak config created </td><td>  -  </td></tr>
     * <tr><td> 400 </td><td> Could not create bucket </td><td>  -  </td></tr>
     * <tr><td> 409 </td><td> Vault with this ID or bucket with this name already exists </td><td>  -  </td></tr>
     * <tr><td> 410 </td><td> Storage profile is archived </td><td>  -  </td></tr>
     * <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
     * <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     * </table>
     */
    public ApiResponse<Void> apiStorageVaultIdPutWithHttpInfo(UUID vaultId, CreateS3STSBucketDto createS3STSBucketDto) throws ApiException {
        Object localVarPostBody = createS3STSBucketDto;

        // verify the required parameter 'vaultId' is set
        if(vaultId == null) {
            throw new ApiException(400, "Missing the required parameter 'vaultId' when calling apiStorageVaultIdPut");
        }

        // create path and map variables
        String localVarPath = "/api/storage/{vaultId}"
                .replaceAll("\\{" + "vaultId" + "\\}", apiClient.escapeString(vaultId.toString()));

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        return apiClient.invokeAPI("StorageResourceApi.apiStorageVaultIdPut", localVarPath, "PUT", localVarQueryParams, localVarPostBody,
                localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
}
