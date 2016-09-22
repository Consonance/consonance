package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecTask   {
  
  private String description = null;
  private List<Ga4ghTaskExecDockerExecutor> docker = new ArrayList<Ga4ghTaskExecDockerExecutor>();
  private List<Ga4ghTaskExecTaskParameter> inputs = new ArrayList<Ga4ghTaskExecTaskParameter>();
  private String name = null;
  private List<Ga4ghTaskExecTaskParameter> outputs = new ArrayList<Ga4ghTaskExecTaskParameter>();
  private String projectId = null;
  private Ga4ghTaskExecResources resources = null;
  private String taskId = null;

  /**
   **/
  public Ga4ghTaskExecTask description(String description) {
    this.description = description;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   **/
  public Ga4ghTaskExecTask docker(List<Ga4ghTaskExecDockerExecutor> docker) {
    this.docker = docker;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("docker")
  public List<Ga4ghTaskExecDockerExecutor> getDocker() {
    return docker;
  }
  public void setDocker(List<Ga4ghTaskExecDockerExecutor> docker) {
    this.docker = docker;
  }

  /**
   **/
  public Ga4ghTaskExecTask inputs(List<Ga4ghTaskExecTaskParameter> inputs) {
    this.inputs = inputs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("inputs")
  public List<Ga4ghTaskExecTaskParameter> getInputs() {
    return inputs;
  }
  public void setInputs(List<Ga4ghTaskExecTaskParameter> inputs) {
    this.inputs = inputs;
  }

  /**
   **/
  public Ga4ghTaskExecTask name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   **/
  public Ga4ghTaskExecTask outputs(List<Ga4ghTaskExecTaskParameter> outputs) {
    this.outputs = outputs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("outputs")
  public List<Ga4ghTaskExecTaskParameter> getOutputs() {
    return outputs;
  }
  public void setOutputs(List<Ga4ghTaskExecTaskParameter> outputs) {
    this.outputs = outputs;
  }

  /**
   **/
  public Ga4ghTaskExecTask projectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("projectId")
  public String getProjectId() {
    return projectId;
  }
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  /**
   **/
  public Ga4ghTaskExecTask resources(Ga4ghTaskExecResources resources) {
    this.resources = resources;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("resources")
  public Ga4ghTaskExecResources getResources() {
    return resources;
  }
  public void setResources(Ga4ghTaskExecResources resources) {
    this.resources = resources;
  }

  /**
   **/
  public Ga4ghTaskExecTask taskId(String taskId) {
    this.taskId = taskId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("taskId")
  public String getTaskId() {
    return taskId;
  }
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecTask ga4ghTaskExecTask = (Ga4ghTaskExecTask) o;
    return Objects.equals(description, ga4ghTaskExecTask.description) &&
        Objects.equals(docker, ga4ghTaskExecTask.docker) &&
        Objects.equals(inputs, ga4ghTaskExecTask.inputs) &&
        Objects.equals(name, ga4ghTaskExecTask.name) &&
        Objects.equals(outputs, ga4ghTaskExecTask.outputs) &&
        Objects.equals(projectId, ga4ghTaskExecTask.projectId) &&
        Objects.equals(resources, ga4ghTaskExecTask.resources) &&
        Objects.equals(taskId, ga4ghTaskExecTask.taskId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, docker, inputs, name, outputs, projectId, resources, taskId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecTask {\n");
    
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    docker: ").append(toIndentedString(docker)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    taskId: ").append(toIndentedString(taskId)).append("\n");
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

