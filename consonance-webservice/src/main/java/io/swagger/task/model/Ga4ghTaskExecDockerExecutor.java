package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T20:13:58.346Z")
public class Ga4ghTaskExecDockerExecutor   {
  
  private List<String> cmd = new ArrayList<String>();
  private String imageName = null;

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
        Objects.equals(imageName, ga4ghTaskExecDockerExecutor.imageName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cmd, imageName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecDockerExecutor {\n");
    
    sb.append("    cmd: ").append(toIndentedString(cmd)).append("\n");
    sb.append("    imageName: ").append(toIndentedString(imageName)).append("\n");
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

