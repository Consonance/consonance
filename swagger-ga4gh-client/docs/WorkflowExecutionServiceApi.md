# WorkflowExecutionServiceApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**cancelJob**](WorkflowExecutionServiceApi.md#cancelJob) | **DELETE** /ga4gh/wes/v1/workflows/{workflow_id} | Cancel a running workflow
[**getServiceInfo**](WorkflowExecutionServiceApi.md#getServiceInfo) | **GET** /ga4gh/wes/v1/service-info | Get information about Workflow Execution Service.  May include information related (but not limited to) the workflow descriptor formats, versions supported, the WES API versions supported, and information about general the service availability.
[**getWorkflowLog**](WorkflowExecutionServiceApi.md#getWorkflowLog) | **GET** /ga4gh/wes/v1/workflows/{workflow_id} | Get detailed info about a running workflow
[**getWorkflowStatus**](WorkflowExecutionServiceApi.md#getWorkflowStatus) | **GET** /ga4gh/wes/v1/workflows/{workflow_id}/status | Get quick status info about a running workflow
[**listWorkflows**](WorkflowExecutionServiceApi.md#listWorkflows) | **GET** /ga4gh/wes/v1/workflows | List the workflows, this endpoint will list the workflows in order of oldest to newest.  There is no guarantee of live updates as the user traverses the pages, the behavior should be decided (and documented) by each implementation.
[**runWorkflow**](WorkflowExecutionServiceApi.md#runWorkflow) | **POST** /ga4gh/wes/v1/workflows | Run a workflow, this endpoint will allow you to create a new workflow request and retrieve its tracking ID to monitor its progress.  An important assumption in this endpoint is that the workflow_params JSON will include parameterizations along with input and output files.  The latter two may be on S3, Google object storage, local filesystems, etc.  This specification makes no distinction.  However, it is assumed that the submitter is using URLs that this system both understands and can access. For Amazon S3, this could be accomplished by given the credentials associated with a WES service access to a particular bucket.  The details are important for a production system and user on-boarding but outside the scope of this spec.


<a name="cancelJob"></a>
# **cancelJob**
> Ga4ghWesWorkflowRunId cancelJob(workflowId)

Cancel a running workflow

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.WorkflowExecutionServiceApi;


WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
String workflowId = "workflowId_example"; // String | 
try {
    Ga4ghWesWorkflowRunId result = apiInstance.cancelJob(workflowId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowExecutionServiceApi#cancelJob");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workflowId** | **String**|  |

### Return type

[**Ga4ghWesWorkflowRunId**](Ga4ghWesWorkflowRunId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getServiceInfo"></a>
# **getServiceInfo**
> Ga4ghWesServiceInfo getServiceInfo()

Get information about Workflow Execution Service.  May include information related (but not limited to) the workflow descriptor formats, versions supported, the WES API versions supported, and information about general the service availability.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.WorkflowExecutionServiceApi;


WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
try {
    Ga4ghWesServiceInfo result = apiInstance.getServiceInfo();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowExecutionServiceApi#getServiceInfo");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Ga4ghWesServiceInfo**](Ga4ghWesServiceInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getWorkflowLog"></a>
# **getWorkflowLog**
> Ga4ghWesWorkflowLog getWorkflowLog(workflowId)

Get detailed info about a running workflow

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.WorkflowExecutionServiceApi;


WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
String workflowId = "workflowId_example"; // String | 
try {
    Ga4ghWesWorkflowLog result = apiInstance.getWorkflowLog(workflowId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowExecutionServiceApi#getWorkflowLog");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workflowId** | **String**|  |

### Return type

[**Ga4ghWesWorkflowLog**](Ga4ghWesWorkflowLog.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getWorkflowStatus"></a>
# **getWorkflowStatus**
> Ga4ghWesWorkflowStatus getWorkflowStatus(workflowId)

Get quick status info about a running workflow

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.WorkflowExecutionServiceApi;


WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
String workflowId = "workflowId_example"; // String | 
try {
    Ga4ghWesWorkflowStatus result = apiInstance.getWorkflowStatus(workflowId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowExecutionServiceApi#getWorkflowStatus");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **workflowId** | **String**|  |

### Return type

[**Ga4ghWesWorkflowStatus**](Ga4ghWesWorkflowStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="listWorkflows"></a>
# **listWorkflows**
> Ga4ghWesWorkflowListResponse listWorkflows(pageSize, pageToken, keyValueSearch)

List the workflows, this endpoint will list the workflows in order of oldest to newest.  There is no guarantee of live updates as the user traverses the pages, the behavior should be decided (and documented) by each implementation.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.WorkflowExecutionServiceApi;


WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
Long pageSize = 789L; // Long | OPTIONAL Number of workflows to return at once. Defaults to 256, and max is 2048.
String pageToken = "pageToken_example"; // String | OPTIONAL Token to use to indicate where to start getting results. If unspecified, returns the first page of results.
String keyValueSearch = "keyValueSearch_example"; // String | OPTIONAL For each key, if the key's value is empty string then match workflows that are tagged with this key regardless of value.
try {
    Ga4ghWesWorkflowListResponse result = apiInstance.listWorkflows(pageSize, pageToken, keyValueSearch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowExecutionServiceApi#listWorkflows");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **pageSize** | **Long**| OPTIONAL Number of workflows to return at once. Defaults to 256, and max is 2048. | [optional]
 **pageToken** | **String**| OPTIONAL Token to use to indicate where to start getting results. If unspecified, returns the first page of results. | [optional]
 **keyValueSearch** | **String**| OPTIONAL For each key, if the key&#39;s value is empty string then match workflows that are tagged with this key regardless of value. | [optional]

### Return type

[**Ga4ghWesWorkflowListResponse**](Ga4ghWesWorkflowListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="runWorkflow"></a>
# **runWorkflow**
> Ga4ghWesWorkflowRunId runWorkflow(body)

Run a workflow, this endpoint will allow you to create a new workflow request and retrieve its tracking ID to monitor its progress.  An important assumption in this endpoint is that the workflow_params JSON will include parameterizations along with input and output files.  The latter two may be on S3, Google object storage, local filesystems, etc.  This specification makes no distinction.  However, it is assumed that the submitter is using URLs that this system both understands and can access. For Amazon S3, this could be accomplished by given the credentials associated with a WES service access to a particular bucket.  The details are important for a production system and user on-boarding but outside the scope of this spec.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.WorkflowExecutionServiceApi;


WorkflowExecutionServiceApi apiInstance = new WorkflowExecutionServiceApi();
Ga4ghWesWorkflowRequest body = new Ga4ghWesWorkflowRequest(); // Ga4ghWesWorkflowRequest | 
try {
    Ga4ghWesWorkflowRunId result = apiInstance.runWorkflow(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WorkflowExecutionServiceApi#runWorkflow");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Ga4ghWesWorkflowRequest**](Ga4ghWesWorkflowRequest.md)|  |

### Return type

[**Ga4ghWesWorkflowRunId**](Ga4ghWesWorkflowRunId.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

