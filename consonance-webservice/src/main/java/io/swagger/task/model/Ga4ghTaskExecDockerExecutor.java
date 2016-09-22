package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecDockerExecutor   {
  
  private List<String> cmd = new ArrayList<String>();
  private String imageName = null;
  private String stderr = null;
  private String stdout = null;

  /**
   **/
  public Ga4ghTaskExecDockerExecutor cmd(List<String> cmd) {
    this.cmd = cmd;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("cmd")
  public List<String> getCmd() {
    return cmd;
  }
  public void setCmd(List<String> cmd) {
    this.cmd = cmd;
  }

  /**
   **/
  public Ga4ghTaskExecDockerExecutor imageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("imageName")
  public String getImageName() {
    return imageName;
  }
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  /**
   **/
  public Ga4ghTaskExecDockerExecutor stderr(String stderr) {
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
  public Ga4ghTaskExecDockerExecutor stdout(String stdout) {
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
    Ga4ghTaskExecDockerExecutor ga4ghTaskExecDockerExecutor = (Ga4ghTaskExecDockerExecutor) o;
    return Objects.equals(cmd, ga4ghTaskExecDockerExecutor.cmd) &&
        Objects.equals(imageName, ga4ghTaskExecDockerExecutor.imageName) &&
        Objects.equals(stderr, ga4ghTaskExecDockerExecutor.stderr) &&
        Objects.equals(stdout, ga4ghTaskExecDockerExecutor.stdout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cmd, imageName, stderr, stdout);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecDockerExecutor {\n");
    
    sb.append("    cmd: ").append(toIndentedString(cmd)).append("\n");
    sb.append("    imageName: ").append(toIndentedString(imageName)).append("\n");
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

