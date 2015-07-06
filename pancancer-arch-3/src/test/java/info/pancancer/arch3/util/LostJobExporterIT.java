/*
 * Copyright (C) 2015 CancerCollaboratory
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.pancancer.arch3.util;

import com.google.common.io.Files;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.ITUtilities;
import info.pancancer.arch3.utils.Utilities;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import joptsimple.OptionException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class LostJobExporterIT {

    @Before
    public void setup() throws IOException, TimeoutException {
        ITUtilities.clearState();
    }

    /**
     * Test of main method, of class Coordinator.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = OptionException.class)
    public void testMainUsage() throws Exception {
        LostJobExporter.main(new String[] { "--help" });
    }

    /**
     * Test of doWork method, of class Coordinator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testNormalUsage() throws Exception {
        File tempDir = Files.createTempDir();
        File config = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(config.getAbsolutePath());
        PostgreSQL db = new PostgreSQL(parseConfig);

        db.createJob(createJob("id1", JobState.LOST));
        db.createJob(createJob("id2", JobState.FAILED));

        // prime the pump with a job
        LostJobExporter.main(new String[] { "--config", config.getAbsolutePath(), "--job_uuids", "id1,id2", "--ini-dir",
                tempDir.getAbsolutePath() });

        Assert.assertTrue("could not find two failed jobs", db.getJobs(JobState.FAILED).size() == 2);
    }

    /**
     * Test of doWork method, of class Coordinator.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFailedUsage() throws Exception {
        File tempDir = Files.createTempDir();
        File config = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(config.getAbsolutePath());
        PostgreSQL db = new PostgreSQL(parseConfig);

        db.createJob(createJob("id1", JobState.RUNNING));

        // prime the pump with a job
        LostJobExporter.main(new String[] { "--config", config.getAbsolutePath(), "--job_uuids", "id1,id2", "--ini-dir",
                tempDir.getAbsolutePath() });
    }

    private Job createJob(String uuid, JobState state) {
        Utilities u = new Utilities();
        String hashStr = u.digest(uuid);
        HashMap<String, String> hm = new HashMap<>();
        hm.put("param1", "bar");
        hm.put("param2", "foo");
        Job job1 = new Job("DEWrapperWorkflow", "1.0.0", "/path/to/workflow", hashStr, hm);
        job1.setUuid(uuid);
        job1.setState(state);
        return job1;
    }
}
