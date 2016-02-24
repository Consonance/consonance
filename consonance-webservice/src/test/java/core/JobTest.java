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

package core;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.consonance.arch.beans.Job;
import io.dropwizard.jackson.Jackson;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static io.dropwizard.testing.FixtureHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author dyuen
 */
public class JobTest {
        private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

        @Test
        public void serializesToJSON() throws Exception {
                final Job job = getJob();
                final String expected = MAPPER.writeValueAsString(
                        MAPPER.readValue(fixture("fixtures/job.json"), Job.class));
                assertThat(MAPPER.writeValueAsString(job)).isEqualTo(expected);
        }

        @Test
        public void deserializesFromJSON() throws Exception {
                final Job person = getJob();
                assertThat(MAPPER.readValue(fixture("fixtures/job.json"), Job.class))
                        .isEqualTo(person);
        }

        public static Job getJob() {
                final Job job = new Job();
                job.setUuid("42");
                job.setEndUser("Player1");
                job.setContainerImageDescriptor("{\n" + "\n" + "    \"items\": [\n" + "        {\n" + "            \"index\": 1,\n"
                        + "            \"index_start_at\": 56,\n" + "            \"integer\": 19,\n" + "            \"float\": 15.1507,\n"
                        + "            \"name\": \"Ashley\",\n" + "            \"surname\": \"Coley\",\n"
                        + "            \"fullname\": \"Brenda Raynor\",\n" + "            \"email\": \"anita@poole.sy\",\n"
                        + "            \"bool\": true\n" + "        }\n" + "    ]\n" + "\n" + "}");
                job.setContainerRuntimeDescriptor("{\n" + "\n" + "    \"items\": [\n" + "        {\n" + "            \"index\": 1,\n"
                        + "            \"index_start_at\": 56,\n" + "            \"integer\": 1,\n" + "            \"float\": 18.5884,\n"
                        + "            \"name\": \"Lee\",\n" + "            \"surname\": \"Summers\",\n"
                        + "            \"fullname\": \"Sandra Alexander\",\n" + "            \"email\": \"ronnie@byrne.gh\",\n"
                        + "            \"bool\": false\n" + "        }\n" + "    ]\n" + "\n" + "}");
                job.setCreateTimestamp(new Timestamp(0));
                job.setUpdateTimestamp(new Timestamp(0));
                Map<String, Job.ExtraFile> extraFiles = new HashMap<>();
                extraFiles.put("/etc/awesomefile/foo.txt", new Job.ExtraFile("contents", false));
                job.setExtraFiles(extraFiles);
                job.setStdout("My god");
                job.setStderr("It's full of stars");
                job.setFlavour("m1.funky");
                return job;
        }
}
