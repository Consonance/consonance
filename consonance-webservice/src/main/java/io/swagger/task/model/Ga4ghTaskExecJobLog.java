package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecJobLog   {
  
  private String commandLine = null;
  private String endTime = null;
  private Integer exitCode = null;
  private String startTime = null;
  private String stderr = null;
  private String stdout = null;

  /**
   **/
  public Ga4ghTaskExecJobLog commandLine(String commandLine) {
    this.commandLine = commandLine;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("commandLine")
  public String getCommandLine() {
    return commandLine;
  }
  public void setCommandLine(String commandLine) {
    this.commandLine = commandLine;
  }

  /**
   **/
  public Ga4ghTaskExecJobLog endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  /**
   **/
  public Ga4ghTaskExecJobLog exitCode(Integer exitCode) {
    this.exitCode = exitCode;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("exitCode")
  public Integer getExitCode() {
    return exitCode;
  }
  public void setExitCode(Integer exitCode) {
    this.exitCode = exitCode;
  }

  /**
   **/
  public Ga4ghTaskExecJobLog startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  /**
   **/
  public Ga4ghTaskExecJobLog stderr(String stderr) {
    this.stderr = stderr;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("stderr")
  public String getStderr() {
    return stderr;
  }
  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  /**
   **/
  public Ga4ghTaskExecJobLog stdout(String stdout) {
    this.stdout = stdout;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("stdout")
  public String getStdout() {
    return stdout;
  }
  public void setStdout(String stdout) {
    this.stdout = stdout;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecJobLog ga4ghTaskExecJobLog = (Ga4ghTaskExecJobLog) o;
    return Objects.equals(commandLine, ga4ghTaskExecJobLog.commandLine) &&
        Objects.equals(endTime, ga4ghTaskExecJobLog.endTime) &&
        Objects.equals(exitCode, ga4ghTaskExecJobLog.exitCode) &&
        Objects.equals(startTime, ga4ghTaskExecJobLog.startTime) &&
        Objects.equals(stderr, ga4ghTaskExecJobLog.stderr) &&
        Objects.equals(stdout, ga4ghTaskExecJobLog.stdout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandLine, endTime, exitCode, startTime, stderr, stdout);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecJobLog {\n");
    
    sb.append("    commandLine: ").append(toIndentedString(commandLine)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    exitCode: ").append(toIndentedString(exitCode)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    stderr: ").append(toIndentedString(stderr)).append("\n");
    sb.append("    stdout: ").append(toIndentedString(stdout)).append("\n");
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

