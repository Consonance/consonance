package io.consonance.arch.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author boconnor
 * @author dyuen
 */
@Entity
@Table(name= "job")
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value="Job", description="Describes jobs running in Consonance")
@NamedQueries({
        @NamedQuery(
                name = "io.consonance.arch.beans.core.Job.findAll",
                query = "SELECT j FROM Job j"
        ),
        @NamedQuery(
                name = "io.consonance.arch.beans.core.Job.findAllByUser",
                query = "SELECT j FROM Job j WHERE endUser LIKE :endUser"
        ),
        @NamedQuery(
                name = "io.consonance.arch.beans.core.Job.findByJobUUID",
                query = "SELECT j FROM Job j WHERE uuid = :jobuuid"
        )
})
@JsonNaming(PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy.class)
public class Job extends BaseBean{

    private static Logger log = LoggerFactory.getLogger(Job.class);

    @ApiModelProperty(value = "job id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="job_id")
    private int jobId;
    @Enumerated(EnumType.STRING)
    @JsonProperty
    @ApiModelProperty(value = "the state of the job ")
    @Column(name = "status", columnDefinition="text")
    private JobState state = JobState.START;
    @JsonProperty("job_uuid")
    @ApiModelProperty(value = "consonance will assign a uuid to jobs")
    @Column(name="job_uuid", columnDefinition="text")
    private String uuid = UUID.randomUUID().toString().toLowerCase();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("workflow_name")
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    @Column(columnDefinition="text")
    private String workflow;
    @JsonProperty("vmuuid")
    @ApiModelProperty(value = "the cloud instance-id assigned to run a job")
    @Column(name="provision_uuid",columnDefinition="text")
    private String vmUuid;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    @Column(name="workflow_version",columnDefinition="text")
    private String workflowVersion;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    @Column(name="workflow_path",columnDefinition="text")
    private String workflowPath;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "can be used to group user-submitted jobs for reporting purposes")
    @Column(name="job_hash",columnDefinition="text")
    private String jobHash;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "used by consonance internally")
    @Column(name="message_type",columnDefinition="text")
    private String messageType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    @Column(name="ini",columnDefinition="text")
    private String iniFileAsString;

    @JsonProperty("arguments")
    @ElementCollection(fetch = FetchType.EAGER, targetClass = String.class)
    @MapKeyColumn(name="key", columnDefinition = "text")
    @Column(name="value", columnDefinition = "text")
    @CollectionTable(name="ini_params", joinColumns=@JoinColumn(name="job_id"))
    @ApiModelProperty(value = "deprecated, key values for seqware workflows")
    private Map<String, String> ini = new HashMap<>();

    @Embeddable
    public static class ExtraFile{
        @ApiModelProperty(value = "contents of the extra files, should not be returned over the webservice")
        @Column(name="content",columnDefinition="text")
	@JsonIgnore
        private String contents;
        @ApiModelProperty(value = "whether to keep this file after workflow execution")
        @Column(name="keep")
        private boolean keep;

        public ExtraFile(){

        }

        public ExtraFile(String contents, boolean keep){
            this.contents = contents;
            this.keep = keep;
        }

	@JsonProperty
	public void setContents(String contents){
	    this.contents = contents;
	}

	@JsonIgnore
        public String getContents() {
            return contents;
        }

        public boolean isKeep() {
            return keep;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ExtraFile other = (ExtraFile) obj;
            return Objects.equals(this.keep, other.keep) && Objects.equals(this.contents, other.contents);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keep, contents);
        }
    }
    @ElementCollection(fetch = FetchType.EAGER, targetClass=ExtraFile.class)
    @MapKeyColumn(name="path", columnDefinition = "text")
    @Column(name="content",columnDefinition = "text")
    @CollectionTable(name="extra_files", joinColumns=@JoinColumn(name="job_id"))
    @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path:keep=content")
    private Map<String, ExtraFile> extraFiles = new HashMap<>();

    @ApiModelProperty(value = "stdout from the job run")
    @Column(columnDefinition="text")
    private String stdout;
    @ApiModelProperty(value = "stderr from the job run")
    @Column(columnDefinition="text")
    private String stderr;
    @JsonProperty("container_image_descriptor")
    @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
    @Column(name="container_image_descriptor",columnDefinition="text")
    private String containerImageDescriptor;
    @JsonProperty("container_runtime_descriptor")
    @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
    @Column(name="container_runtime_descriptor",columnDefinition="text")
    private String containerRuntimeDescriptor;
    @ApiModelProperty(value = "indicates the user that scheduled a job", required=true)
    @Column(name="end_user",columnDefinition="text")
    private String endUser;
    @ApiModelProperty(value = "indicates the flavour of VM for a job", required=true)
    @Column(columnDefinition="text")
    private String flavour = null;

    public Job(String workflow, String workflowVersion, String workflowPath, String jobHash, Map<String, String> ini) {
        this.workflow = workflow;
        this.workflowVersion = workflowVersion;
        this.workflowPath = workflowPath;
        this.jobHash = jobHash;
        this.ini = ini;
    }

    public Job() {
        super();
    }

    /**
     * This sucks, can't figure out how to do this with generics.
     * @param json
     * @return
     */
    public Job fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.readValue(json, Job.class);
        } catch (JsonParseException e) {
            log.error("JSON parsing error: ", e.getMessage());
            return null;
        } catch (IOException e) {
            log.error("IO exception parsing error: ", e.getMessage());
            return null;
        }
    }


    public String getEndUser() {
        return endUser;
    }

    public void setEndUser(String endUser) {
        this.endUser = endUser;
    }


    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public Map<String, String> getIni() {
        return ini;
    }

    public Map<String, Job.ExtraFile> getExtraFiles() {
        return extraFiles;
    }


    public void setExtraFiles(Map<String, ExtraFile> ini) {
        this.extraFiles = ini;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "deprecated, read-only convenience renderer for ini files")
    public String getIniStr() {
        StringBuilder sb = new StringBuilder();
        for (String key : this.ini.keySet()) {
            sb.append(key).append("=").append(this.ini.get(key)).append("\n");
        }
        return (sb.toString());
    }

    public void setIni(Map<String, String> ini) {
        this.ini = ini;
    }

    public String getJobHash() {
        return jobHash;
    }

    public void setJobHash(String jobHash) {
        this.jobHash = jobHash;
    }

    public String getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(String workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public String getWorkflowPath() {
        return workflowPath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    /**
     * @return the stdout
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * @param stdout
     *            the stdout to set
     */
    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    /**
     * @return the stderr
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * @param stderr
     *            the stderr to set
     */
    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public int getJobId() {
        return jobId;
    }


    public String getContainerImageDescriptor() {
        return containerImageDescriptor;
    }

    public void setContainerImageDescriptor(String containerImageDescriptor) {
        this.containerImageDescriptor = containerImageDescriptor;
    }

    public String getContainerRuntimeDescriptor() {
        return containerRuntimeDescriptor;
    }

    public void setContainerRuntimeDescriptor(String containerRuntimeDescriptor) {
        this.containerRuntimeDescriptor = containerRuntimeDescriptor;
    }

    public String getIniFileAsString() {
        return iniFileAsString;
    }

    public void setIniFileAsString(String iniFileAsString) {
        this.iniFileAsString = iniFileAsString;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, state, uuid, vmUuid, messageType, extraFiles, stdout, stderr, containerImageDescriptor,
                containerRuntimeDescriptor, endUser, flavour);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Job other = (Job) obj;
        return Objects.equals(this.jobId, other.jobId) && Objects.equals(this.state, other.state) && Objects.equals(this.uuid, other.uuid)
                && Objects.equals(this.vmUuid, other.vmUuid) && Objects.equals(this.messageType, other.messageType)
                && Objects.equals(this.extraFiles, other.extraFiles) && Objects.equals(this.stdout, other.stdout)
                && Objects.equals(this.stderr, other.stderr)
                && Objects.equals(this.containerImageDescriptor, other.containerImageDescriptor)
                && Objects.equals(this.containerRuntimeDescriptor, other.containerRuntimeDescriptor)
                && Objects.equals(this.endUser, other.endUser) && Objects.equals(this.flavour, other.flavour);
    }
}
