/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.swagger.workflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T18:39:51.024Z")
public class JobStatus   {
  
  private String run = null;
  private Object input = null;
  private String output = null;

  /**
   * Current status for the job
   */
  public enum StateEnum {
    RUNNING("Running"),

        PAUSED("Paused"),

        SUCCESS("Success"),

        FAILED("Failed"),

        CANCELLED("Cancelled");
    private String value;

    StateEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }
  }

  private StateEnum state = null;
  private String log = null;

  /**
   * URL to the descriptor that was run
   **/
  public JobStatus run(String run) {
    this.run = run;
    return this;
  }

  
  @ApiModelProperty(value = "URL to the descriptor that was run")
  @JsonProperty("run")
  public String getRun() {
    return run;
  }
  public void setRun(String run) {
    this.run = run;
  }

  /**
   * JSON parameter file for the run
   **/
  public JobStatus input(Object input) {
    this.input = input;
    return this;
  }

  
  @ApiModelProperty(value = "JSON parameter file for the run")
  @JsonProperty("input")
  public Object getInput() {
    return input;
  }
  public void setInput(Object input) {
    this.input = input;
  }

  /**
   * Snippet of stdout?
   **/
  public JobStatus output(String output) {
    this.output = output;
    return this;
  }

  
  @ApiModelProperty(value = "Snippet of stdout?")
  @JsonProperty("output")
  public String getOutput() {
    return output;
  }
  public void setOutput(String output) {
    this.output = output;
  }

  /**
   * Current status for the job
   **/
  public JobStatus state(StateEnum state) {
    this.state = state;
    return this;
  }

  
  @ApiModelProperty(value = "Current status for the job")
  @JsonProperty("state")
  public StateEnum getState() {
    return state;
  }
  public void setState(StateEnum state) {
    this.state = state;
  }

  /**
   * URL to get just he log for this job
   **/
  public JobStatus log(String log) {
    this.log = log;
    return this;
  }

  
  @ApiModelProperty(value = "URL to get just he log for this job")
  @JsonProperty("log")
  public String getLog() {
    return log;
  }
  public void setLog(String log) {
    this.log = log;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobStatus jobStatus = (JobStatus) o;
    return Objects.equals(run, jobStatus.run) &&
        Objects.equals(input, jobStatus.input) &&
        Objects.equals(output, jobStatus.output) &&
        Objects.equals(state, jobStatus.state) &&
        Objects.equals(log, jobStatus.log);
  }

  @Override
  public int hashCode() {
    return Objects.hash(run, input, output, state, log);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobStatus {\n");
    
    sb.append("    run: ").append(toIndentedString(run)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    log: ").append(toIndentedString(log)).append("\n");
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

