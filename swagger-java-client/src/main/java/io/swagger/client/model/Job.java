package io.swagger.client.model;

import io.swagger.client.StringUtil;
import java.util.Date;
import java.util.Map;
import java.util.*;



import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Describes jobs running in Consonance
 **/
@ApiModel(description = "Describes jobs running in Consonance")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-09-24T17:17:13.241Z")
public class Job   {
  
  private Long jobId = null;

public enum StateEnum {
  START("START"), PENDING("PENDING"), RUNNING("RUNNING"), SUCCESS("SUCCESS"), FAILED("FAILED"), LOST("LOST");

  private String value;

  StateEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

  private StateEnum state = null;
  private String workflow = null;
  private String jobHash = null;
  private String messageType = null;
  private Map<String, String> extraFiles = new HashMap<String, String>();
  private Date createTs = null;
  private Date updateTs = null;
  private String stdout = null;
  private String stderr = null;
  private String containerImageDescriptor = null;
  private String containerRuntimeDescriptor = null;
  private String endUser = null;
  private String flavour = null;
  private String iniStr = null;
  private String jobUuid = null;
  private String vmuuid = null;
  private Map<String, String> arguments = new HashMap<String, String>();

  
  /**
   * job id
   **/
  @ApiModelProperty(value = "job id")
  @JsonProperty("jobId")
  public Long getJobId() {
    return jobId;
  }
  public void setJobId(Long jobId) {
    this.jobId = jobId;
  }

  
  /**
   * the state of the job
   **/
  @ApiModelProperty(value = "the state of the job")
  @JsonProperty("state")
  public StateEnum getState() {
    return state;
  }
  public void setState(StateEnum state) {
    this.state = state;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("workflow")
  public String getWorkflow() {
    return workflow;
  }
  public void setWorkflow(String workflow) {
    this.workflow = workflow;
  }

  
  /**
   * can be used to group user-submitted jobs for reporting purposes
   **/
  @ApiModelProperty(value = "can be used to group user-submitted jobs for reporting purposes")
  @JsonProperty("jobHash")
  public String getJobHash() {
    return jobHash;
  }
  public void setJobHash(String jobHash) {
    this.jobHash = jobHash;
  }

  
  /**
   * used by consonance internally
   **/
  @ApiModelProperty(value = "used by consonance internally")
  @JsonProperty("messageType")
  public String getMessageType() {
    return messageType;
  }
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  
  /**
   * credentials or other files needed by your workflow, specify pairs of path=content
   **/
  @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
  @JsonProperty("extraFiles")
  public Map<String, String> getExtraFiles() {
    return extraFiles;
  }
  public void setExtraFiles(Map<String, String> extraFiles) {
    this.extraFiles = extraFiles;
  }

  
  /**
   * the time a job was submitted
   **/
  @ApiModelProperty(value = "the time a job was submitted")
  @JsonProperty("createTs")
  public Date getCreateTs() {
    return createTs;
  }
  public void setCreateTs(Date createTs) {
    this.createTs = createTs;
  }

  
  /**
   * the last time we saw a job
   **/
  @ApiModelProperty(value = "the last time we saw a job")
  @JsonProperty("updateTs")
  public Date getUpdateTs() {
    return updateTs;
  }
  public void setUpdateTs(Date updateTs) {
    this.updateTs = updateTs;
  }

  
  /**
   * stdout from the job run
   **/
  @ApiModelProperty(value = "stdout from the job run")
  @JsonProperty("stdout")
  public String getStdout() {
    return stdout;
  }
  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  
  /**
   * stderr from the job run
   **/
  @ApiModelProperty(value = "stderr from the job run")
  @JsonProperty("stderr")
  public String getStderr() {
    return stderr;
  }
  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  
  /**
   * credentials or other files needed by your workflow, specify pairs of path=content
   **/
  @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
  @JsonProperty("containerImageDescriptor")
  public String getContainerImageDescriptor() {
    return containerImageDescriptor;
  }
  public void setContainerImageDescriptor(String containerImageDescriptor) {
    this.containerImageDescriptor = containerImageDescriptor;
  }

  
  /**
   * credentials or other files needed by your workflow, specify pairs of path=content
   **/
  @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
  @JsonProperty("containerRuntimeDescriptor")
  public String getContainerRuntimeDescriptor() {
    return containerRuntimeDescriptor;
  }
  public void setContainerRuntimeDescriptor(String containerRuntimeDescriptor) {
    this.containerRuntimeDescriptor = containerRuntimeDescriptor;
  }

  
  /**
   * indicates the user that scheduled a job
   **/
  @ApiModelProperty(required = true, value = "indicates the user that scheduled a job")
  @JsonProperty("endUser")
  public String getEndUser() {
    return endUser;
  }
  public void setEndUser(String endUser) {
    this.endUser = endUser;
  }

  
  /**
   * indicates the flavour of VM for a job
   **/
  @ApiModelProperty(required = true, value = "indicates the flavour of VM for a job")
  @JsonProperty("flavour")
  public String getFlavour() {
    return flavour;
  }
  public void setFlavour(String flavour) {
    this.flavour = flavour;
  }

  
  /**
   * deprecated, read-only convenience renderer for ini files
   **/
  @ApiModelProperty(value = "deprecated, read-only convenience renderer for ini files")
  @JsonProperty("iniStr")
  public String getIniStr() {
    return iniStr;
  }
  public void setIniStr(String iniStr) {
    this.iniStr = iniStr;
  }

  
  /**
   * consonance will assign a uuid to jobs
   **/
  @ApiModelProperty(value = "consonance will assign a uuid to jobs")
  @JsonProperty("job_uuid")
  public String getJobUuid() {
    return jobUuid;
  }
  public void setJobUuid(String jobUuid) {
    this.jobUuid = jobUuid;
  }

  
  /**
   * the cloud instance-id assigned to run a job
   **/
  @ApiModelProperty(value = "the cloud instance-id assigned to run a job")
  @JsonProperty("vmuuid")
  public String getVmuuid() {
    return vmuuid;
  }
  public void setVmuuid(String vmuuid) {
    this.vmuuid = vmuuid;
  }

  
  /**
   * deprecated, key values for seqware workflows
   **/
  @ApiModelProperty(value = "deprecated, key values for seqware workflows")
  @JsonProperty("arguments")
  public Map<String, String> getArguments() {
    return arguments;
  }
  public void setArguments(Map<String, String> arguments) {
    this.arguments = arguments;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Job {\n");
    
    sb.append("    jobId: ").append(StringUtil.toIndentedString(jobId)).append("\n");
    sb.append("    state: ").append(StringUtil.toIndentedString(state)).append("\n");
    sb.append("    workflow: ").append(StringUtil.toIndentedString(workflow)).append("\n");
    sb.append("    jobHash: ").append(StringUtil.toIndentedString(jobHash)).append("\n");
    sb.append("    messageType: ").append(StringUtil.toIndentedString(messageType)).append("\n");
    sb.append("    extraFiles: ").append(StringUtil.toIndentedString(extraFiles)).append("\n");
    sb.append("    createTs: ").append(StringUtil.toIndentedString(createTs)).append("\n");
    sb.append("    updateTs: ").append(StringUtil.toIndentedString(updateTs)).append("\n");
    sb.append("    stdout: ").append(StringUtil.toIndentedString(stdout)).append("\n");
    sb.append("    stderr: ").append(StringUtil.toIndentedString(stderr)).append("\n");
    sb.append("    containerImageDescriptor: ").append(StringUtil.toIndentedString(containerImageDescriptor)).append("\n");
    sb.append("    containerRuntimeDescriptor: ").append(StringUtil.toIndentedString(containerRuntimeDescriptor)).append("\n");
    sb.append("    endUser: ").append(StringUtil.toIndentedString(endUser)).append("\n");
    sb.append("    flavour: ").append(StringUtil.toIndentedString(flavour)).append("\n");
    sb.append("    iniStr: ").append(StringUtil.toIndentedString(iniStr)).append("\n");
    sb.append("    jobUuid: ").append(StringUtil.toIndentedString(jobUuid)).append("\n");
    sb.append("    vmuuid: ").append(StringUtil.toIndentedString(vmuuid)).append("\n");
    sb.append("    arguments: ").append(StringUtil.toIndentedString(arguments)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
