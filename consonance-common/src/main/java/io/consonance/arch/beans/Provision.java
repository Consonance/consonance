package io.consonance.arch.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.IOException;
import java.util.List;

/**
 * @author dyuen
 */
@Entity
@Table(name= "provision")
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value="provision", description="Describes provision requests in Consonance, needs to be deprecated")
@JsonNaming(PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy.class)
public class Provision extends BaseBean{

    private static Logger log = LoggerFactory.getLogger(Provision.class);

    @ApiModelProperty(value = "provision id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="provision_id")
    private int provisionId;
    @ApiModelProperty(value = "deprecated, the number of cores for this VM")
    @Column(columnDefinition="integer")
    private long cores;
    @ApiModelProperty(value = "deprecated, the amount of memory for this VM")
    @Column(name = "mem_gb", columnDefinition="integer")
    private long memGb;
    @ApiModelProperty(value = "deprecated, the amount of storage for this VM")
    @Column(name = "storage_gb", columnDefinition="integer")
    private long storageGb;
    @ApiModelProperty(value = "the state of the provision ")
    @Column(name = "status", columnDefinition="text")
    private ProvisionState state = ProvisionState.START;
    @JsonProperty("bindle_profiles_to_run")
    @ElementCollection(targetClass = String.class)
    @ApiModelProperty(value = "deprecated, ansible playbook to run on provisioned instances", hidden=true)
    private List<String> ansiblePlaybooks;
    @ApiModelProperty(value = "the state of the provision ")
    @Column(name="ip_address",columnDefinition="text")
    private String ipAddress = "";
    @ApiModelProperty(value = "uuid for the job")
    @Column(name="job_uuid",columnDefinition="text")
    private String jobUUID = "";
    /**
     * This is the provision_uuid
     */
    @ApiModelProperty(value = "uuid for the instance, should be the instance id on Amazon")
    @Column(name="provision_uuid",columnDefinition="text")
    private String provisionUUID = "";


    public Provision(int cores, int memGb, int storageGb, List<String> ansiblePlaybooks) {
        this.cores = cores;
        this.memGb = memGb;
        this.storageGb = storageGb;
        this.ansiblePlaybooks = ansiblePlaybooks;
    }

    public Provision() {
        super();
    }

    /**
     * This sucks, can't figure out how to do this with generics.
     * @param json
     * @return
     */
    public Provision fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.readValue(json, Provision.class);
        } catch (JsonParseException e) {
            log.error("JSON parsing error: ", e.getMessage());
            return null;
        } catch (IOException e) {
            log.error("IO exception parsing error: ", e.getMessage());
            return null;
        }
    }

    public String getProvisionUUID() {
        return provisionUUID;
    }

    public void setProvisionUUID(String provisionUUID) {
        this.provisionUUID = provisionUUID;
    }

    public long getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(long storageGb) {
        this.storageGb = storageGb;
    }

    public List<String> getAnsiblePlaybooks() {
        return ansiblePlaybooks;
    }

    public void setAnsiblePlaybooks(List<String> ansiblePlaybooks) {
        this.ansiblePlaybooks = ansiblePlaybooks;
    }

    public long getMemGb() {
        return memGb;
    }

    public void setMemGb(long memGb) {
        this.memGb = memGb;
    }

    public long getCores() {
        return cores;
    }

    public void setCores(long cores) {
        this.cores = cores;
    }

    public ProvisionState getState() {
        return state;
    }

    public void setState(ProvisionState state) {
        this.state = state;
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress
     *            the ipAddress to set
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * @return the jobUUID
     */
    public String getJobUUID() {
        return jobUUID;
    }

    /**
     * @param jobUUID
     *            the jobUUID to set
     */
    public void setJobUUID(String jobUUID) {
        this.jobUUID = jobUUID;
    }

    public int getProvisionId() {
        return provisionId;
    }

    public void setProvisionId(int provisionId) {
        this.provisionId = provisionId;
    }
}
