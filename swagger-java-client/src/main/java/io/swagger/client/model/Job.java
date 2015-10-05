package io.swagger.client.model;

import io.swagger.client.StringUtil;
import java.util.Date;
import java.util.Map;
import java.util.*;
import io.swagger.client.model.ExtraFile;



import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Describes jobs running in Consonance
 **/
@ApiModel(description = "Describes jobs running in Consonance")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-10-05T18:28:38.975Z")
public class Job   {
  
  private Date createTimestamp = null;
  private Date updateTimestamp = null;
  private Integer jobId = null;

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
  private Map<String, ExtraFile> extraFiles = new HashMap<String, ExtraFile>();
  private String stdout = null;
  private String stderr = null;
  private String endUser = null;
  private String flavour = null;
  private String iniStr = null;
  private String jobUuid = null;
  private String vmuuid = null;
  private Map<String, String> arguments = new HashMap<String, String>();
  private String containerImageDescriptor = null;
  private String containerRuntimeDescriptor = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("create_timestamp")
  public Date getCreateTimestamp() {
    return createTimestamp;
  }
  public void setCreateTimestamp(Date createTimestamp) {
    this.createTimestamp = createTimestamp;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("update_timestamp")
  public Date getUpdateTimestamp() {
    return updateTimestamp;
  }
  public void setUpdateTimestamp(Date updateTimestamp) {
    this.updateTimestamp = updateTimestamp;
  }

  
  /**
   * job id
   **/
  @ApiModelProperty(value = "job id")
  @JsonProperty("job_id")
  public Integer getJobId() {
    return jobId;
  }
  public void setJobId(Integer jobId) {
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
  @JsonProperty("job_hash")
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
  @JsonProperty("message_type")
  public String getMessageType() {
    return messageType;
  }
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  
  /**
   * credentials or other files needed by your workflow, specify pairs of path:keep=content
   **/
  @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path:keep=content")
  @JsonProperty("extra_files")
  public Map<String, ExtraFile> getExtraFiles() {
    return extraFiles;
  }
  public void setExtraFiles(Map<String, ExtraFile> extraFiles) {
    this.extraFiles = extraFiles;
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
   * indicates the user that scheduled a job
   **/
  @ApiModelProperty(required = true, value = "indicates the user that scheduled a job")
  @JsonProperty("end_user")
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
  @JsonProperty("ini_str")
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

  
  /**
   * credentials or other files needed by your workflow, specify pairs of path=content
   **/
  @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
  @JsonProperty("container_image_descriptor")
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
  @JsonProperty("container_runtime_descriptor")
  public String getContainerRuntimeDescriptor() {
    return containerRuntimeDescriptor;
  }
  public void setContainerRuntimeDescriptor(String containerRuntimeDescriptor) {
    this.containerRuntimeDescriptor = containerRuntimeDescriptor;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Job {\n");
    
    sb.append("    createTimestamp: ").append(StringUtil.toIndentedString(createTimestamp)).append("\n");
    sb.append("    updateTimestamp: ").append(StringUtil.toIndentedString(updateTimestamp)).append("\n");
    sb.append("    jobId: ").append(StringUtil.toIndentedString(jobId)).append("\n");
    sb.append("    state: ").append(StringUtil.toIndentedString(state)).append("\n");
    sb.append("    workflow: ").append(StringUtil.toIndentedString(workflow)).append("\n");
    sb.append("    jobHash: ").append(StringUtil.toIndentedString(jobHash)).append("\n");
    sb.append("    messageType: ").append(StringUtil.toIndentedString(messageType)).append("\n");
    sb.append("    extraFiles: ").append(StringUtil.toIndentedString(extraFiles)).append("\n");
    sb.append("    stdout: ").append(StringUtil.toIndentedString(stdout)).append("\n");
    sb.append("    stderr: ").append(StringUtil.toIndentedString(stderr)).append("\n");
    sb.append("    endUser: ").append(StringUtil.toIndentedString(endUser)).append("\n");
    sb.append("    flavour: ").append(StringUtil.toIndentedString(flavour)).append("\n");
    sb.append("    iniStr: ").append(StringUtil.toIndentedString(iniStr)).append("\n");
    sb.append("    jobUuid: ").append(StringUtil.toIndentedString(jobUuid)).append("\n");
    sb.append("    vmuuid: ").append(StringUtil.toIndentedString(vmuuid)).append("\n");
    sb.append("    arguments: ").append(StringUtil.toIndentedString(arguments)).append("\n");
    sb.append("    containerImageDescriptor: ").append(StringUtil.toIndentedString(containerImageDescriptor)).append("\n");
    sb.append("    containerRuntimeDescriptor: ").append(StringUtil.toIndentedString(containerRuntimeDescriptor)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
