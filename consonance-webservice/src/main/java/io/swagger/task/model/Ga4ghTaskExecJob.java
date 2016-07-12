package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecJob   {
  
  private String jobId = null;
  private List<Ga4ghTaskExecJobLog> logs = new ArrayList<Ga4ghTaskExecJobLog>();
  private Map<String, String> metadata = new HashMap<String, String>();
  private Ga4ghTaskExecState state = null;
  private Ga4ghTaskExecTask task = null;

  /**
   **/
  public Ga4ghTaskExecJob jobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("jobId")
  public String getJobId() {
    return jobId;
  }
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  /**
   **/
  public Ga4ghTaskExecJob logs(List<Ga4ghTaskExecJobLog> logs) {
    this.logs = logs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("logs")
  public List<Ga4ghTaskExecJobLog> getLogs() {
    return logs;
  }
  public void setLogs(List<Ga4ghTaskExecJobLog> logs) {
    this.logs = logs;
  }

  /**
   **/
  public Ga4ghTaskExecJob metadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("metadata")
  public Map<String, String> getMetadata() {
    return metadata;
  }
  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  /**
   **/
  public Ga4ghTaskExecJob state(Ga4ghTaskExecState state) {
    this.state = state;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("state")
  public Ga4ghTaskExecState getState() {
    return state;
  }
  public void setState(Ga4ghTaskExecState state) {
    this.state = state;
  }

  /**
   **/
  public Ga4ghTaskExecJob task(Ga4ghTaskExecTask task) {
    this.task = task;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("task")
  public Ga4ghTaskExecTask getTask() {
    return task;
  }
  public void setTask(Ga4ghTaskExecTask task) {
    this.task = task;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecJob ga4ghTaskExecJob = (Ga4ghTaskExecJob) o;
    return Objects.equals(jobId, ga4ghTaskExecJob.jobId) &&
        Objects.equals(logs, ga4ghTaskExecJob.logs) &&
        Objects.equals(metadata, ga4ghTaskExecJob.metadata) &&
        Objects.equals(state, ga4ghTaskExecJob.state) &&
        Objects.equals(task, ga4ghTaskExecJob.task);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId, logs, metadata, state, task);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecJob {\n");
    
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    logs: ").append(toIndentedString(logs)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    task: ").append(toIndentedString(task)).append("\n");
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

