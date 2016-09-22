package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;



/**
 * Attached volume request.
 **/

@ApiModel(description = "Attached volume request.")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecVolume   {
  
  private String mountPoint = null;
  private String name = null;
  private Long sizeGb = null;
  private String source = null;

  /**
   **/
  public Ga4ghTaskExecVolume mountPoint(String mountPoint) {
    this.mountPoint = mountPoint;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("mountPoint")
  public String getMountPoint() {
    return mountPoint;
  }
  public void setMountPoint(String mountPoint) {
    this.mountPoint = mountPoint;
  }

  /**
   **/
  public Ga4ghTaskExecVolume name(String name) {
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
  public Ga4ghTaskExecVolume sizeGb(Long sizeGb) {
    this.sizeGb = sizeGb;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("sizeGb")
  public Long getSizeGb() {
    return sizeGb;
  }
  public void setSizeGb(Long sizeGb) {
    this.sizeGb = sizeGb;
  }

  /**
   **/
  public Ga4ghTaskExecVolume source(String source) {
    this.source = source;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("source")
  public String getSource() {
    return source;
  }
  public void setSource(String source) {
    this.source = source;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecVolume ga4ghTaskExecVolume = (Ga4ghTaskExecVolume) o;
    return Objects.equals(mountPoint, ga4ghTaskExecVolume.mountPoint) &&
        Objects.equals(name, ga4ghTaskExecVolume.name) &&
        Objects.equals(sizeGb, ga4ghTaskExecVolume.sizeGb) &&
        Objects.equals(source, ga4ghTaskExecVolume.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mountPoint, name, sizeGb, source);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecVolume {\n");
    
    sb.append("    mountPoint: ").append(toIndentedString(mountPoint)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    sizeGb: ").append(toIndentedString(sizeGb)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
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

