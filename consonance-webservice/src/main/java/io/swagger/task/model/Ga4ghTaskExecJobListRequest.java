package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T20:13:58.346Z")
public class Ga4ghTaskExecJobListRequest   {
  
  private String namePrefix = null;
  private Long pageSize = null;
  private String pageToken = null;
  private String projectId = null;

  /**
   * Pipelines with names that match this prefix should be returned. If unspecified, all pipelines in the project, up to pageSize, will be returned.
   **/
  public Ga4ghTaskExecJobListRequest namePrefix(String namePrefix) {
    this.namePrefix = namePrefix;
    return this;
  }

  
  @ApiModelProperty(value = "Pipelines with names that match this prefix should be returned. If unspecified, all pipelines in the project, up to pageSize, will be returned.")
  @JsonProperty("namePrefix")
  public String getNamePrefix() {
    return namePrefix;
  }
  public void setNamePrefix(String namePrefix) {
    this.namePrefix = namePrefix;
  }

  /**
   * Number of pipelines to return at once. Defaults to 256, and max is 2048.
   **/
  public Ga4ghTaskExecJobListRequest pageSize(Long pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  
  @ApiModelProperty(value = "Number of pipelines to return at once. Defaults to 256, and max is 2048.")
  @JsonProperty("pageSize")
  public Long getPageSize() {
    return pageSize;
  }
  public void setPageSize(Long pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Token to use to indicate where to start getting results. If unspecified, returns the first page of results.
   **/
  public Ga4ghTaskExecJobListRequest pageToken(String pageToken) {
    this.pageToken = pageToken;
    return this;
  }

  
  @ApiModelProperty(value = "Token to use to indicate where to start getting results. If unspecified, returns the first page of results.")
  @JsonProperty("pageToken")
  public String getPageToken() {
    return pageToken;
  }
  public void setPageToken(String pageToken) {
    this.pageToken = pageToken;
  }

  /**
   * Required. The name of the project to search for pipelines. Caller must have READ access to this project.
   **/
  public Ga4ghTaskExecJobListRequest projectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  
  @ApiModelProperty(value = "Required. The name of the project to search for pipelines. Caller must have READ access to this project.")
  @JsonProperty("projectId")
  public String getProjectId() {
    return projectId;
  }
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecJobListRequest ga4ghTaskExecJobListRequest = (Ga4ghTaskExecJobListRequest) o;
    return Objects.equals(namePrefix, ga4ghTaskExecJobListRequest.namePrefix) &&
        Objects.equals(pageSize, ga4ghTaskExecJobListRequest.pageSize) &&
        Objects.equals(pageToken, ga4ghTaskExecJobListRequest.pageToken) &&
        Objects.equals(projectId, ga4ghTaskExecJobListRequest.projectId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namePrefix, pageSize, pageToken, projectId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecJobListRequest {\n");
    
    sb.append("    namePrefix: ").append(toIndentedString(namePrefix)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    pageToken: ").append(toIndentedString(pageToken)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

