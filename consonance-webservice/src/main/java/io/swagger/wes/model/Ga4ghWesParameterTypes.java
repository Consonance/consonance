/*
 * workflow_execution.proto
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: version not set
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.wes.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets ga4gh_wes_parameter_types
 */
public enum Ga4ghWesParameterTypes {
  
  DIRECTORY("Directory"),
  
  FILE("File"),
  
  PARAMETER("Parameter");

  private String value;

  Ga4ghWesParameterTypes(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Ga4ghWesParameterTypes fromValue(String text) {
    for (Ga4ghWesParameterTypes b : Ga4ghWesParameterTypes.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

