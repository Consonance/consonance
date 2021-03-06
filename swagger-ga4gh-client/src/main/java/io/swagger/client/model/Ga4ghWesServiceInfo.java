/*
 * workflow_execution.proto
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: version not set
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.Ga4ghWesWorkflowTypeVersion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * .
 */
@ApiModel(description = ".")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-12-07T15:00:20.150-08:00")
public class Ga4ghWesServiceInfo {
  @SerializedName("workflow_type_versions")
  private Map<String, Ga4ghWesWorkflowTypeVersion> workflowTypeVersions = null;

  @SerializedName("supported_wes_versions")
  private List<String> supportedWesVersions = null;

  @SerializedName("supported_filesystem_protocols")
  private List<String> supportedFilesystemProtocols = null;

  @SerializedName("engine_versions")
  private Map<String, String> engineVersions = null;

  @SerializedName("system_state_counts")
  private Map<String, Long> systemStateCounts = null;

  @SerializedName("key_values")
  private Map<String, String> keyValues = null;

  public Ga4ghWesServiceInfo workflowTypeVersions(Map<String, Ga4ghWesWorkflowTypeVersion> workflowTypeVersions) {
    this.workflowTypeVersions = workflowTypeVersions;
    return this;
  }

  public Ga4ghWesServiceInfo putWorkflowTypeVersionsItem(String key, Ga4ghWesWorkflowTypeVersion workflowTypeVersionsItem) {
    if (this.workflowTypeVersions == null) {
      this.workflowTypeVersions = new HashMap<String, Ga4ghWesWorkflowTypeVersion>();
    }
    this.workflowTypeVersions.put(key, workflowTypeVersionsItem);
    return this;
  }

   /**
   * Get workflowTypeVersions
   * @return workflowTypeVersions
  **/
  @ApiModelProperty(value = "")
  public Map<String, Ga4ghWesWorkflowTypeVersion> getWorkflowTypeVersions() {
    return workflowTypeVersions;
  }

  public void setWorkflowTypeVersions(Map<String, Ga4ghWesWorkflowTypeVersion> workflowTypeVersions) {
    this.workflowTypeVersions = workflowTypeVersions;
  }

  public Ga4ghWesServiceInfo supportedWesVersions(List<String> supportedWesVersions) {
    this.supportedWesVersions = supportedWesVersions;
    return this;
  }

  public Ga4ghWesServiceInfo addSupportedWesVersionsItem(String supportedWesVersionsItem) {
    if (this.supportedWesVersions == null) {
      this.supportedWesVersions = new ArrayList<String>();
    }
    this.supportedWesVersions.add(supportedWesVersionsItem);
    return this;
  }

   /**
   * Get supportedWesVersions
   * @return supportedWesVersions
  **/
  @ApiModelProperty(value = "")
  public List<String> getSupportedWesVersions() {
    return supportedWesVersions;
  }

  public void setSupportedWesVersions(List<String> supportedWesVersions) {
    this.supportedWesVersions = supportedWesVersions;
  }

  public Ga4ghWesServiceInfo supportedFilesystemProtocols(List<String> supportedFilesystemProtocols) {
    this.supportedFilesystemProtocols = supportedFilesystemProtocols;
    return this;
  }

  public Ga4ghWesServiceInfo addSupportedFilesystemProtocolsItem(String supportedFilesystemProtocolsItem) {
    if (this.supportedFilesystemProtocols == null) {
      this.supportedFilesystemProtocols = new ArrayList<String>();
    }
    this.supportedFilesystemProtocols.add(supportedFilesystemProtocolsItem);
    return this;
  }

   /**
   * The filesystem protocols supported by this service, currently these may include common protocols such as &#39;http&#39;, &#39;https&#39;, &#39;sftp&#39;, &#39;s3&#39;, &#39;gs&#39;, &#39;file&#39;, &#39;synapse&#39;, or others as supported by this service.
   * @return supportedFilesystemProtocols
  **/
  @ApiModelProperty(value = "The filesystem protocols supported by this service, currently these may include common protocols such as 'http', 'https', 'sftp', 's3', 'gs', 'file', 'synapse', or others as supported by this service.")
  public List<String> getSupportedFilesystemProtocols() {
    return supportedFilesystemProtocols;
  }

  public void setSupportedFilesystemProtocols(List<String> supportedFilesystemProtocols) {
    this.supportedFilesystemProtocols = supportedFilesystemProtocols;
  }

  public Ga4ghWesServiceInfo engineVersions(Map<String, String> engineVersions) {
    this.engineVersions = engineVersions;
    return this;
  }

  public Ga4ghWesServiceInfo putEngineVersionsItem(String key, String engineVersionsItem) {
    if (this.engineVersions == null) {
      this.engineVersions = new HashMap<String, String>();
    }
    this.engineVersions.put(key, engineVersionsItem);
    return this;
  }

   /**
   * Get engineVersions
   * @return engineVersions
  **/
  @ApiModelProperty(value = "")
  public Map<String, String> getEngineVersions() {
    return engineVersions;
  }

  public void setEngineVersions(Map<String, String> engineVersions) {
    this.engineVersions = engineVersions;
  }

  public Ga4ghWesServiceInfo systemStateCounts(Map<String, Long> systemStateCounts) {
    this.systemStateCounts = systemStateCounts;
    return this;
  }

  public Ga4ghWesServiceInfo putSystemStateCountsItem(String key, Long systemStateCountsItem) {
    if (this.systemStateCounts == null) {
      this.systemStateCounts = new HashMap<String, Long>();
    }
    this.systemStateCounts.put(key, systemStateCountsItem);
    return this;
  }

   /**
   * The system statistics, key is the statistic, value is the count of workflows in that state. See the State enum for the possible keys.
   * @return systemStateCounts
  **/
  @ApiModelProperty(value = "The system statistics, key is the statistic, value is the count of workflows in that state. See the State enum for the possible keys.")
  public Map<String, Long> getSystemStateCounts() {
    return systemStateCounts;
  }

  public void setSystemStateCounts(Map<String, Long> systemStateCounts) {
    this.systemStateCounts = systemStateCounts;
  }

  public Ga4ghWesServiceInfo keyValues(Map<String, String> keyValues) {
    this.keyValues = keyValues;
    return this;
  }

  public Ga4ghWesServiceInfo putKeyValuesItem(String key, String keyValuesItem) {
    if (this.keyValues == null) {
      this.keyValues = new HashMap<String, String>();
    }
    this.keyValues.put(key, keyValuesItem);
    return this;
  }

   /**
   * Get keyValues
   * @return keyValues
  **/
  @ApiModelProperty(value = "")
  public Map<String, String> getKeyValues() {
    return keyValues;
  }

  public void setKeyValues(Map<String, String> keyValues) {
    this.keyValues = keyValues;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghWesServiceInfo ga4ghWesServiceInfo = (Ga4ghWesServiceInfo) o;
    return Objects.equals(this.workflowTypeVersions, ga4ghWesServiceInfo.workflowTypeVersions) &&
        Objects.equals(this.supportedWesVersions, ga4ghWesServiceInfo.supportedWesVersions) &&
        Objects.equals(this.supportedFilesystemProtocols, ga4ghWesServiceInfo.supportedFilesystemProtocols) &&
        Objects.equals(this.engineVersions, ga4ghWesServiceInfo.engineVersions) &&
        Objects.equals(this.systemStateCounts, ga4ghWesServiceInfo.systemStateCounts) &&
        Objects.equals(this.keyValues, ga4ghWesServiceInfo.keyValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowTypeVersions, supportedWesVersions, supportedFilesystemProtocols, engineVersions, systemStateCounts, keyValues);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghWesServiceInfo {\n");
    
    sb.append("    workflowTypeVersions: ").append(toIndentedString(workflowTypeVersions)).append("\n");
    sb.append("    supportedWesVersions: ").append(toIndentedString(supportedWesVersions)).append("\n");
    sb.append("    supportedFilesystemProtocols: ").append(toIndentedString(supportedFilesystemProtocols)).append("\n");
    sb.append("    engineVersions: ").append(toIndentedString(engineVersions)).append("\n");
    sb.append("    systemStateCounts: ").append(toIndentedString(systemStateCounts)).append("\n");
    sb.append("    keyValues: ").append(toIndentedString(keyValues)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
}

