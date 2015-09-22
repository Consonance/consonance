package io.consonance.arch.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author boconnor
 * @author dyuen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value="Job", description="Describes jobs running in Consonance")
@NamedQueries({
        @NamedQuery(
                name = "io.consonance.arch.beans.core.Job.findAll",
                query = "SELECT j FROM Job j"
        ),
        @NamedQuery(
                name = "io.consonance.arch.beans.core.Job.findAllByUser",
                query = "SELECT j FROM Job j where endUser LIKE :endUser"
        )
})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="job_id")
    private long jobId;
    private JobState state = JobState.START;
    private String uuid = UUID.randomUUID().toString().toLowerCase();
    private String workflow;
    private String vmUuid;
    private String workflowVersion;
    private String workflowPath;
    private String jobHash;
    private String messageType;
    private Map<String, String> ini = new HashMap<>();
    private Map<String, String> extraFiles = new HashMap<>();
    private Timestamp createTs;
    private Timestamp updateTs;
    private String stdout;
    private String stderr;
    private String endUser;
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

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Job fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.readValue(json, Job.class);
        } catch (JsonParseException e) {
            // TODO: improve logging for JSON parse errors.
            System.out.println("JSON parsing error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

    @JsonProperty
    @ApiModelProperty(value = "indicates the user that scheduled a job", required=true)
    public String getEndUser() {
        return endUser;
    }

    public void setEndUser(String endUser) {
        this.endUser = endUser;
    }

    @JsonProperty
    @ApiModelProperty(value = "indicates the flavour of VM for a job", required=true)
    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    @JsonProperty("job_uuid")
    @ApiModelProperty(value = "consonance will assign a uuid to jobs")
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @JsonProperty("arguments")
    @ApiModelProperty(value = "seqware containers use ini files, deprecated")
    public Map<String, String> getIni() {
        return ini;
    }

    @JsonProperty("extra_files")
    @ApiModelProperty(value = "credentials or other files needed by your workflow, specify pairs of path=content")
    public Map<String, String> getExtraFiles() {
        return extraFiles;
    }

    @JsonIgnore
    public String getExtraFilesStr() {
        StringBuilder sb = new StringBuilder();
        for (String key : this.extraFiles.keySet()) {
            sb.append(key).append("=").append(this.extraFiles.get(key)).append("\n");
        }
        return (sb.toString());
    }

    public void setExtraFiles(Map<String, String> ini) {
        this.extraFiles = ini;
    }

    @JsonIgnore
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

    @JsonProperty("job_hash")
    @ApiModelProperty(value = "can be used to group user-submitted jobs for reporting purposes")
    public String getJobHash() {
        return jobHash;
    }

    public void setJobHash(String jobHash) {
        this.jobHash = jobHash;
    }

    @JsonProperty("workflow_version")
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    public String getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(String workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    @JsonProperty("workflow_name")
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    @JsonProperty
    @ApiModelProperty(value = "the state of the job ")
    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    @JsonProperty("workflow_path")
    @ApiModelProperty(value = "used by seqware, deprecated", hidden=true)
    public String getWorkflowPath() {
        return workflowPath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }

    @JsonProperty("message_type")
    @ApiModelProperty(value = "used by consonance internally")
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @JsonProperty("create_ts")
    @ApiModelProperty(value = "the time a job was submitted")
    public Timestamp getCreateTs() {
        return createTs;
    }

    public void setCreateTs(Timestamp createTs) {
        this.createTs = createTs;
    }

    @JsonProperty("update_ts")
    @ApiModelProperty(value = "the last time we saw a job")
    public Timestamp getUpdateTs() {
        return updateTs;
    }

    public void setUpdateTs(Timestamp updateTs) {
        this.updateTs = updateTs;
    }

    @JsonProperty("vmuuid")
    @ApiModelProperty(value = "the cloud instance-id assigned to run a job")
    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    /**
     * @return the stdout
     */
    @ApiModelProperty(value = "stdout from the job run")
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
    @ApiModelProperty(value = "stderr from the job run")
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

    @JsonProperty("job_id")
    @ApiModelProperty(value = "job id")
    public long getJobId() {
        return jobId;
    }
}
