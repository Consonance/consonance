package io.swagger.client.model;

import io.swagger.client.StringUtil;
import java.util.Date;



import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-10-22T21:18:50.332Z")
public class ConsonanceUser   {
  
  private Date createTimestamp = null;
  private Date updateTimestamp = null;
  private Integer userId = null;
  private String name = null;
  private Boolean admin = null;
  private String hashedPassword = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("createTimestamp")
  public Date getCreateTimestamp() {
    return createTimestamp;
  }
  public void setCreateTimestamp(Date createTimestamp) {
    this.createTimestamp = createTimestamp;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("updateTimestamp")
  public Date getUpdateTimestamp() {
    return updateTimestamp;
  }
  public void setUpdateTimestamp(Date updateTimestamp) {
    this.updateTimestamp = updateTimestamp;
  }

  
  /**
   * db id
   **/
  @ApiModelProperty(value = "db id")
  @JsonProperty("user_id")
  public Integer getUserId() {
    return userId;
  }
  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  
  /**
   * username
   **/
  @ApiModelProperty(value = "username")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  
  /**
   * set this if creating an admin
   **/
  @ApiModelProperty(value = "set this if creating an admin")
  @JsonProperty("admin")
  public Boolean getAdmin() {
    return admin;
  }
  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  
  /**
   * the hash token, can be used to identify the user
   **/
  @ApiModelProperty(value = "the hash token, can be used to identify the user")
  @JsonProperty("hashedPassword")
  public String getHashedPassword() {
    return hashedPassword;
  }
  public void setHashedPassword(String hashedPassword) {
    this.hashedPassword = hashedPassword;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsonanceUser {\n");
    
    sb.append("    createTimestamp: ").append(StringUtil.toIndentedString(createTimestamp)).append("\n");
    sb.append("    updateTimestamp: ").append(StringUtil.toIndentedString(updateTimestamp)).append("\n");
    sb.append("    userId: ").append(StringUtil.toIndentedString(userId)).append("\n");
    sb.append("    name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("    admin: ").append(StringUtil.toIndentedString(admin)).append("\n");
    sb.append("    hashedPassword: ").append(StringUtil.toIndentedString(hashedPassword)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
