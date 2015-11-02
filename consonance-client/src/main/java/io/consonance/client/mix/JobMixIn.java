package io.consonance.client.mix;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.client.model.ExtraFile;
import io.swagger.client.model.Job;

import java.util.Map;

/**
 * @author dyuen
 */
@JsonIgnoreProperties({ "extra_files", "create_timestamp", "update_timestamp"
,"job_id", "container_image_descriptor", "container_runtime_descriptor", "end_user", "stdout", "stderr"})
public interface JobMixIn {

        @JsonProperty("job_uuid")
        String getJobUuid();

        @JsonProperty("flavour")
        String getFlavour();

        @JsonProperty("state")
        Job.StateEnum getState();

        @JsonIgnore()
        @JsonProperty("extra_files")
        Map<String, ExtraFile> getExtraFiles();
}
