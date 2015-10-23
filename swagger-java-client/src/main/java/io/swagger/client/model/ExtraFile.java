package io.swagger.client.model;

import io.swagger.client.StringUtil;



import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-10-22T21:18:50.332Z")
public class ExtraFile   {
  
  private String contents = null;
  private Boolean keep = null;

  
  /**
   * contents of the extra files, should not be returned over the webservice
   **/
  @ApiModelProperty(value = "contents of the extra files, should not be returned over the webservice")
  @JsonProperty("contents")
  public String getContents() {
    return contents;
  }
  public void setContents(String contents) {
    this.contents = contents;
  }

  
  /**
   * whether to keep this file after workflow execution
   **/
  @ApiModelProperty(value = "whether to keep this file after workflow execution")
  @JsonProperty("keep")
  public Boolean getKeep() {
    return keep;
  }
  public void setKeep(Boolean keep) {
    this.keep = keep;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExtraFile {\n");
    
    sb.append("    contents: ").append(StringUtil.toIndentedString(contents)).append("\n");
    sb.append("    keep: ").append(StringUtil.toIndentedString(keep)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
