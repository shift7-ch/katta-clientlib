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
import ch.iterate.hub.client.model.DeviceDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class DeviceResourceApi {
  private ApiClient apiClient;

  public DeviceResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public DeviceResourceApi(ApiClient apiClient) {
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
   * removes a device
   * the device will be only be removed if the current user is the owner
   * @param deviceId  (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 204 </td><td> device removed </td><td>  -  </td></tr>
  <tr><td> 404 </td><td> device not found with current user </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public void apiDevicesDeviceIdDelete(String deviceId) throws ApiException {
    apiDevicesDeviceIdDeleteWithHttpInfo(deviceId);
  }

  /**
   * removes a device
   * the device will be only be removed if the current user is the owner
   * @param deviceId  (required)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 204 </td><td> device removed </td><td>  -  </td></tr>
  <tr><td> 404 </td><td> device not found with current user </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<Void> apiDevicesDeviceIdDeleteWithHttpInfo(String deviceId) throws ApiException {
    Object localVarPostBody = null;

      // verify the required parameter 'deviceId' is set
      if(deviceId == null) {
      throw new ApiException(400, "Missing the required parameter 'deviceId' when calling apiDevicesDeviceIdDelete");
    }

      // create path and map variables
    String localVarPath = "/api/devices/{deviceId}"
            .replaceAll("\\{" + "deviceId" + "\\}", apiClient.escapeString(deviceId.toString()));

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


      final String[] localVarAccepts = {

      };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

      String[] localVarAuthNames = new String[]{};

    return apiClient.invokeAPI("DeviceResourceApi.apiDevicesDeviceIdDelete", localVarPath, "DELETE", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, null, false);
  }
  /**
   * get the device
   * the device must be owned by the currently logged-in user
   * @param deviceId  (required)
   * @return DeviceDto
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> Device found </td><td>  -  </td></tr>
  <tr><td> 404 </td><td> Device not found or owned by a different user </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public DeviceDto apiDevicesDeviceIdGet(String deviceId) throws ApiException {
    return apiDevicesDeviceIdGetWithHttpInfo(deviceId).getData();
  }

  /**
   * get the device
   * the device must be owned by the currently logged-in user
   * @param deviceId  (required)
   * @return ApiResponse&lt;DeviceDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> Device found </td><td>  -  </td></tr>
  <tr><td> 404 </td><td> Device not found or owned by a different user </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<DeviceDto> apiDevicesDeviceIdGetWithHttpInfo(String deviceId) throws ApiException {
    Object localVarPostBody = null;

      // verify the required parameter 'deviceId' is set
      if(deviceId == null) {
      throw new ApiException(400, "Missing the required parameter 'deviceId' when calling apiDevicesDeviceIdGet");
    }

      // create path and map variables
    String localVarPath = "/api/devices/{deviceId}"
            .replaceAll("\\{" + "deviceId" + "\\}", apiClient.escapeString(deviceId.toString()));

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


      final String[] localVarAccepts = {
              "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

      String[] localVarAuthNames = new String[]{};

      GenericType<DeviceDto> localVarReturnType = new GenericType<DeviceDto>() {
      };

    return apiClient.invokeAPI("DeviceResourceApi.apiDevicesDeviceIdGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, localVarReturnType, false);
  }
  /**
   * list legacy access tokens
   * get all legacy access tokens for this device ({vault1: token1, vault1: token2, ...}). The device must be owned by the currently logged-in user
   * @param deviceId  (required)
   * @return Map&lt;String, String&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   * @deprecated
   */
  @Deprecated
  public Map<String, String> apiDevicesDeviceIdLegacyAccessTokensGet(String deviceId) throws ApiException {
    return apiDevicesDeviceIdLegacyAccessTokensGetWithHttpInfo(deviceId).getData();
  }

  /**
   * list legacy access tokens
   * get all legacy access tokens for this device ({vault1: token1, vault1: token2, ...}). The device must be owned by the currently logged-in user
   * @param deviceId  (required)
   * @return ApiResponse&lt;Map&lt;String, String&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   * @deprecated
   */
  @Deprecated
  public ApiResponse<Map<String, String>> apiDevicesDeviceIdLegacyAccessTokensGetWithHttpInfo(String deviceId) throws ApiException {
    Object localVarPostBody = null;

      // verify the required parameter 'deviceId' is set
      if(deviceId == null) {
      throw new ApiException(400, "Missing the required parameter 'deviceId' when calling apiDevicesDeviceIdLegacyAccessTokensGet");
    }

      // create path and map variables
    String localVarPath = "/api/devices/{deviceId}/legacy-access-tokens"
            .replaceAll("\\{" + "deviceId" + "\\}", apiClient.escapeString(deviceId.toString()));

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


      final String[] localVarAccepts = {
              "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

      String[] localVarAuthNames = new String[]{};

      GenericType<Map<String, String>> localVarReturnType = new GenericType<Map<String, String>>() {
      };

    return apiClient.invokeAPI("DeviceResourceApi.apiDevicesDeviceIdLegacyAccessTokensGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, localVarReturnType, false);
  }
  /**
   * creates or updates a device
   * the device will be owned by the currently logged-in user
   * @param deviceId  (required)
   * @param deviceDto  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 201 </td><td> Device created or updated </td><td>  -  </td></tr>
  <tr><td> 409 </td><td> Device with this key already exists </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public void apiDevicesDeviceIdPut(String deviceId, DeviceDto deviceDto) throws ApiException {
    apiDevicesDeviceIdPutWithHttpInfo(deviceId, deviceDto);
  }

  /**
   * creates or updates a device
   * the device will be owned by the currently logged-in user
   * @param deviceId  (required)
   * @param deviceDto  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 201 </td><td> Device created or updated </td><td>  -  </td></tr>
  <tr><td> 409 </td><td> Device with this key already exists </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<Void> apiDevicesDeviceIdPutWithHttpInfo(String deviceId, DeviceDto deviceDto) throws ApiException {
    Object localVarPostBody = deviceDto;

      // verify the required parameter 'deviceId' is set
      if(deviceId == null) {
      throw new ApiException(400, "Missing the required parameter 'deviceId' when calling apiDevicesDeviceIdPut");
    }

      // create path and map variables
    String localVarPath = "/api/devices/{deviceId}"
            .replaceAll("\\{" + "deviceId" + "\\}", apiClient.escapeString(deviceId.toString()));

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

    return apiClient.invokeAPI("DeviceResourceApi.apiDevicesDeviceIdPut", localVarPath, "PUT", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, null, false);
  }
  /**
   * lists all devices matching the given ids
   * lists for each id in the list its corresponding device. Ignores all id&#39;s where a device cannot be found
   * @param ids  (optional)
   * @return List&lt;DeviceDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public List<DeviceDto> apiDevicesGet(List<String> ids) throws ApiException {
    return apiDevicesGetWithHttpInfo(ids).getData();
  }

  /**
   * lists all devices matching the given ids
   * lists for each id in the list its corresponding device. Ignores all id&#39;s where a device cannot be found
   * @param ids  (optional)
   * @return ApiResponse&lt;List&lt;DeviceDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> OK </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> Not Allowed </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<List<DeviceDto>> apiDevicesGetWithHttpInfo(List<String> ids) throws ApiException {
    Object localVarPostBody = null;

      // create path and map variables
    String localVarPath = "/api/devices";

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

      GenericType<List<DeviceDto>> localVarReturnType = new GenericType<List<DeviceDto>>() {
      };

    return apiClient.invokeAPI("DeviceResourceApi.apiDevicesGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, localVarReturnType, false);
  }
}
