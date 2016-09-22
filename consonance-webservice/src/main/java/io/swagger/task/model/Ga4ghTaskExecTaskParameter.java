package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecTaskParameter   {
  
  private String description = null;
  private Boolean directory = null;
  private String location = null;
  private String name = null;
  private String path = null;

  /**
   **/
  public Ga4ghTaskExecTaskParameter description(String description) {
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
  public Ga4ghTaskExecTaskParameter directory(Boolean directory) {
    this.directory = directory;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("directory")
  public Boolean getDirectory() {
    return directory;
  }
  public void setDirectory(Boolean directory) {
    this.directory = directory;
  }

  /**
   **/
  public Ga4ghTaskExecTaskParameter location(String location) {
    this.location = location;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("location")
  public String getLocation() {
    return location;
  }
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   **/
  public Ga4ghTaskExecTaskParameter name(String name) {
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
  public Ga4ghTaskExecTaskParameter path(String path) {
    this.path = path;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("path")
  public String getPath() {
    return path;
  }
  public void setPath(String path) {
    this.path = path;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecTaskParameter ga4ghTaskExecTaskParameter = (Ga4ghTaskExecTaskParameter) o;
    return Objects.equals(description, ga4ghTaskExecTaskParameter.description) &&
        Objects.equals(directory, ga4ghTaskExecTaskParameter.directory) &&
        Objects.equals(location, ga4ghTaskExecTaskParameter.location) &&
        Objects.equals(name, ga4ghTaskExecTaskParameter.name) &&
        Objects.equals(path, ga4ghTaskExecTaskParameter.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, directory, location, name, path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecTaskParameter {\n");
    
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    directory: ").append(toIndentedString(directory)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
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

