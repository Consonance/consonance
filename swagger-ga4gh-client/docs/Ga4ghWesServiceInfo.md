
# Ga4ghWesServiceInfo

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**workflowTypeVersions** | [**Map&lt;String, Ga4ghWesWorkflowTypeVersion&gt;**](Ga4ghWesWorkflowTypeVersion.md) |  |  [optional]
**supportedWesVersions** | **List&lt;String&gt;** |  |  [optional]
**supportedFilesystemProtocols** | **List&lt;String&gt;** | The filesystem protocols supported by this service, currently these may include common protocols such as &#39;http&#39;, &#39;https&#39;, &#39;sftp&#39;, &#39;s3&#39;, &#39;gs&#39;, &#39;file&#39;, &#39;synapse&#39;, or others as supported by this service. |  [optional]
**engineVersions** | **Map&lt;String, String&gt;** |  |  [optional]
**systemStateCounts** | **Map&lt;String, Long&gt;** | The system statistics, key is the statistic, value is the count of workflows in that state. See the State enum for the possible keys. |  [optional]
**keyValues** | **Map&lt;String, String&gt;** |  |  [optional]



