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
