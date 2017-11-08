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
    @JsonProperty("vm_uuid")
    @ApiModelProperty(value = "the cloud instance-id assigned to run a job")
    @Column(name="provision_uuid",columnDefinition="text")
    private String vmUuid;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "can be used to group user-submitted jobs for reporting purposes")
    @Column(name="job_hash",columnDefinition="text")
    private String jobHash;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "used by consonance internally")
    @Column(name="message_type",columnDefinition="text")
    private String messageType;

    @Embeddable
    public static class ExtraFile{
        @ApiModelProperty(value = "contents of the extra files, should not be returned over the webservice")
        @Column(name="content",columnDefinition="text")
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

	    public void setContents(String contents){
	    this.contents = contents;
	}

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
    @JsonProperty("container_image_descriptor_type")
    @ApiModelProperty(value = "type of descriptor, typically CWL or WDL")
    @Column(name="container_image_descriptor_type",columnDefinition="text")
    private String containerImageDescriptorType;
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

    public Job(String jobHash) {
        this.jobHash = jobHash;
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


    public Map<String, Job.ExtraFile> getExtraFiles() {
        return extraFiles;
    }


    public void setExtraFiles(Map<String, ExtraFile> ini) {
        this.extraFiles = ini;
    }

    public String getJobHash() {
        return jobHash;
    }

    public void setJobHash(String jobHash) {
        this.jobHash = jobHash;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
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

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getContainerImageDescriptor() {
        return containerImageDescriptor;
    }

    public void setContainerImageDescriptor(String containerImageDescriptor) {
        this.containerImageDescriptor = containerImageDescriptor;
    }

    public String getContainerImageDescriptorType() {
        return containerImageDescriptorType;
    }

    public void setContainerImageDescriptorType(String containerImageDescriptorType) {
        this.containerImageDescriptorType = containerImageDescriptorType;
    }

    public String getContainerRuntimeDescriptor() {
        return containerRuntimeDescriptor;
    }

    public void setContainerRuntimeDescriptor(String containerRuntimeDescriptor) {
        this.containerRuntimeDescriptor = containerRuntimeDescriptor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, state, uuid, vmUuid, messageType, extraFiles, stdout, stderr, containerImageDescriptor, containerImageDescriptorType,
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
                && Objects.equals(this.containerImageDescriptorType, other.containerImageDescriptorType)
                && Objects.equals(this.containerRuntimeDescriptor, other.containerRuntimeDescriptor)
                && Objects.equals(this.endUser, other.endUser) && Objects.equals(this.flavour, other.flavour);
    }
}
