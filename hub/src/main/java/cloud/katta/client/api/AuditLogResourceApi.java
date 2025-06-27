package cloud.katta.client.api;

import cloud.katta.client.ApiException;
import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiResponse;
import cloud.katta.client.Configuration;
import cloud.katta.client.Pair;

import javax.ws.rs.core.GenericType;

import cloud.katta.client.model.AuditEventDto;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.14.0")
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
   * @param type the list of type of events to return. Empty list is all events. (optional)
   * @return List&lt;AuditEventDto&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Body contains list of events in the specified time interval </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> startDate or endDate not specified, startDate &gt; endDate, order specified and not in [&#39;asc&#39;,&#39;desc&#39;], pageSize not in [1 .. 100] or type is not valid </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> Community license used or license expired </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> requesting user does not have admin role </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public List<AuditEventDto> apiAuditlogGet(@javax.annotation.Nullable DateTime startDate, @javax.annotation.Nullable DateTime endDate, @javax.annotation.Nullable Long paginationId, @javax.annotation.Nullable String order, @javax.annotation.Nullable Integer pageSize, @javax.annotation.Nullable List<String> type) throws ApiException {
    return apiAuditlogGetWithHttpInfo(startDate, endDate, paginationId, order, pageSize, type).getData();
  }

  /**
   * list all auditlog entries within a period
   * list all auditlog entries from a period specified by a start and end date
   * @param startDate the start date of the period as ISO 8601 datetime string, inclusive (optional)
   * @param endDate the end date of the period as ISO 8601 datetime string, exclusive (optional)
   * @param paginationId The smallest (asc ordering) or highest (desc ordering) audit entry id, not included in results. Used for pagination.  (optional)
   * @param order The order of the queried table. Determines if most recent (desc) or oldest entries (asc) are considered first. Allowed Values are &#39;desc&#39; (default) or &#39;asc&#39;. Used for pagination. (optional, default to desc)
   * @param pageSize the maximum number of entries to return. Must be between 1 and 100. (optional, default to 20)
   * @param type the list of type of events to return. Empty list is all events. (optional)
   * @return ApiResponse&lt;List&lt;AuditEventDto&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Body contains list of events in the specified time interval </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> startDate or endDate not specified, startDate &gt; endDate, order specified and not in [&#39;asc&#39;,&#39;desc&#39;], pageSize not in [1 .. 100] or type is not valid </td><td>  -  </td></tr>
       <tr><td> 402 </td><td> Community license used or license expired </td><td>  -  </td></tr>
       <tr><td> 403 </td><td> requesting user does not have admin role </td><td>  -  </td></tr>
       <tr><td> 401 </td><td> Not Authorized </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<AuditEventDto>> apiAuditlogGetWithHttpInfo(@javax.annotation.Nullable DateTime startDate, @javax.annotation.Nullable DateTime endDate, @javax.annotation.Nullable Long paginationId, @javax.annotation.Nullable String order, @javax.annotation.Nullable Integer pageSize, @javax.annotation.Nullable List<String> type) throws ApiException {
    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "startDate", startDate)
    );
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "endDate", endDate));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "paginationId", paginationId));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "order", order));
    localVarQueryParams.addAll(apiClient.parameterToPairs("", "pageSize", pageSize));
    localVarQueryParams.addAll(apiClient.parameterToPairs("multi", "type", type));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"SecurityScheme"};
    GenericType<List<AuditEventDto>> localVarReturnType = new GenericType<List<AuditEventDto>>() {};
    return apiClient.invokeAPI("AuditLogResourceApi.apiAuditlogGet", "/api/auditlog", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
}
