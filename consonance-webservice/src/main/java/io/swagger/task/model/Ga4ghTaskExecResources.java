package io.swagger.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;





@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class Ga4ghTaskExecResources   {
  
  private Long minimumCpuCores = null;
  private Long minimumRamGb = null;
  private Boolean preemptible = null;
  private List<Ga4ghTaskExecVolume> volumes = new ArrayList<Ga4ghTaskExecVolume>();
  private List<String> zones = new ArrayList<String>();

  /**
   **/
  public Ga4ghTaskExecResources minimumCpuCores(Long minimumCpuCores) {
    this.minimumCpuCores = minimumCpuCores;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("minimumCpuCores")
  public Long getMinimumCpuCores() {
    return minimumCpuCores;
  }
  public void setMinimumCpuCores(Long minimumCpuCores) {
    this.minimumCpuCores = minimumCpuCores;
  }

  /**
   **/
  public Ga4ghTaskExecResources minimumRamGb(Long minimumRamGb) {
    this.minimumRamGb = minimumRamGb;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("minimumRamGb")
  public Long getMinimumRamGb() {
    return minimumRamGb;
  }
  public void setMinimumRamGb(Long minimumRamGb) {
    this.minimumRamGb = minimumRamGb;
  }

  /**
   **/
  public Ga4ghTaskExecResources preemptible(Boolean preemptible) {
    this.preemptible = preemptible;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("preemptible")
  public Boolean getPreemptible() {
    return preemptible;
  }
  public void setPreemptible(Boolean preemptible) {
    this.preemptible = preemptible;
  }

  /**
   **/
  public Ga4ghTaskExecResources volumes(List<Ga4ghTaskExecVolume> volumes) {
    this.volumes = volumes;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("volumes")
  public List<Ga4ghTaskExecVolume> getVolumes() {
    return volumes;
  }
  public void setVolumes(List<Ga4ghTaskExecVolume> volumes) {
    this.volumes = volumes;
  }

  /**
   **/
  public Ga4ghTaskExecResources zones(List<String> zones) {
    this.zones = zones;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("zones")
  public List<String> getZones() {
    return zones;
  }
  public void setZones(List<String> zones) {
    this.zones = zones;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ga4ghTaskExecResources ga4ghTaskExecResources = (Ga4ghTaskExecResources) o;
    return Objects.equals(minimumCpuCores, ga4ghTaskExecResources.minimumCpuCores) &&
        Objects.equals(minimumRamGb, ga4ghTaskExecResources.minimumRamGb) &&
        Objects.equals(preemptible, ga4ghTaskExecResources.preemptible) &&
        Objects.equals(volumes, ga4ghTaskExecResources.volumes) &&
        Objects.equals(zones, ga4ghTaskExecResources.zones);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minimumCpuCores, minimumRamGb, preemptible, volumes, zones);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ga4ghTaskExecResources {\n");
    
    sb.append("    minimumCpuCores: ").append(toIndentedString(minimumCpuCores)).append("\n");
    sb.append("    minimumRamGb: ").append(toIndentedString(minimumRamGb)).append("\n");
    sb.append("    preemptible: ").append(toIndentedString(preemptible)).append("\n");
    sb.append("    volumes: ").append(toIndentedString(volumes)).append("\n");
    sb.append("    zones: ").append(toIndentedString(zones)).append("\n");
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

