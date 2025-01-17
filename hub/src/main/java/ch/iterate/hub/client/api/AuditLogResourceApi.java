package ch.iterate.hub.client.api;

import org.joda.time.DateTime;

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
import ch.iterate.hub.client.model.AuditEventDto;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
public class AuditLogResourceApi {
  private ApiClient apiClient;

  public AuditLogResourceApi() {
    this(Configuration.getDefaultApiClient());
  }

  public AuditLogResourceApi(ApiClient apiClient) {
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
   * list all auditlog entries within a period
   * list all auditlog entries from a period specified by a start and end date
   * @param startDate the start date of the period as ISO 8601 datetime string, inclusive (optional)
   * @param endDate the end date of the period as ISO 8601 datetime string, exclusive (optional)
   * @param paginationId The smallest (asc ordering) or highest (desc ordering) audit entry id, not included in results. Used for pagination.  (optional)
   * @param order The order of the queried table. Determines if most recent (desc) or oldest entries (asc) are considered first. Allowed Values are &#39;desc&#39; (default) or &#39;asc&#39;. Used for pagination. (optional, default to desc)
   * @param pageSize the maximum number of entries to return. Must be between 1 and 100. (optional, default to 20)
   * @return List&lt;AuditEventDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> Body contains list of events in the specified time interval </td><td>  -  </td></tr>
  <tr><td> 400 </td><td> startDate or endDate not specified, startDate &gt; endDate, order specified and not in [&#39;asc&#39;,&#39;desc&#39;] or pageSize not in [1 .. 100] </td><td>  -  </td></tr>
  <tr><td> 402 </td><td> Community license used or license expired </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> requesting user does not have admin role </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public List<AuditEventDto> apiAuditlogGet(DateTime startDate, DateTime endDate, Long paginationId, String order, Integer pageSize) throws ApiException {
    return apiAuditlogGetWithHttpInfo(startDate, endDate, paginationId, order, pageSize).getData();
  }

  /**
   * list all auditlog entries within a period
   * list all auditlog entries from a period specified by a start and end date
   * @param startDate the start date of the period as ISO 8601 datetime string, inclusive (optional)
   * @param endDate the end date of the period as ISO 8601 datetime string, exclusive (optional)
   * @param paginationId The smallest (asc ordering) or highest (desc ordering) audit entry id, not included in results. Used for pagination.  (optional)
   * @param order The order of the queried table. Determines if most recent (desc) or oldest entries (asc) are considered first. Allowed Values are &#39;desc&#39; (default) or &#39;asc&#39;. Used for pagination. (optional, default to desc)
   * @param pageSize the maximum number of entries to return. Must be between 1 and 100. (optional, default to 20)
   * @return ApiResponse&lt;List&lt;AuditEventDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
  <table summary="Response Details" border="1">
  <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
  <tr><td> 200 </td><td> Body contains list of events in the specified time interval </td><td>  -  </td></tr>
  <tr><td> 400 </td><td> startDate or endDate not specified, startDate &gt; endDate, order specified and not in [&#39;asc&#39;,&#39;desc&#39;] or pageSize not in [1 .. 100] </td><td>  -  </td></tr>
  <tr><td> 402 </td><td> Community license used or license expired </td><td>  -  </td></tr>
  <tr><td> 403 </td><td> requesting user does not have admin role </td><td>  -  </td></tr>
  <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
  </table>
   */
  public ApiResponse<List<AuditEventDto>> apiAuditlogGetWithHttpInfo(DateTime startDate, DateTime endDate, Long paginationId, String order, Integer pageSize) throws ApiException {
    Object localVarPostBody = null;

      // create path and map variables
    String localVarPath = "/api/auditlog";

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    localVarQueryParams.addAll(apiClient.parameterToPairs("", "startDate", startDate));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "endDate", endDate));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "paginationId", paginationId));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "order", order));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "pageSize", pageSize));


      final String[] localVarAccepts = {
              "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

      String[] localVarAuthNames = new String[]{};

      GenericType<List<AuditEventDto>> localVarReturnType = new GenericType<List<AuditEventDto>>() {
      };

    return apiClient.invokeAPI("AuditLogResourceApi.apiAuditlogGet", localVarPath, "GET", localVarQueryParams, localVarPostBody,
            localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType,
            localVarAuthNames, localVarReturnType, false);
  }
}
