package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecJobListResponse   {
  
  private List<Ga4ghTaskExecJob> jobs = new ArrayList<Ga4ghTaskExecJob>();
  private String nextPageToken = null;

  /**
   **/
  public Ga4ghTaskExecJobListResponse jobs(List<Ga4ghTaskExecJob> jobs) {
    this.jobs = jobs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("jobs")
  public List<Ga4ghTaskExecJob> getJobs() {
    return jobs;
  }
  public void setJobs(List<Ga4ghTaskExecJob> jobs) {
    this.jobs = jobs;
  }

  /**
   **/
  public Ga4ghTaskExecJobListResponse nextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("nextPageToken")
  public String getNextPageToken() {
    return nextPageToken;
  }
  public void setNextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecJobListResponse ga4ghTaskExecJobListResponse = (Ga4ghTaskExecJobListResponse) o;
    return Objects.equals(jobs, ga4ghTaskExecJobListResponse.jobs) &&
        Objects.equals(nextPageToken, ga4ghTaskExecJobListResponse.nextPageToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobs, nextPageToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecJobListResponse {\n");
    
    sb.append("    jobs: ").append(toIndentedString(jobs)).append("\n");
    sb.append("    nextPageToken: ").append(toIndentedString(nextPageToken)).append("\n");
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

