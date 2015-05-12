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
package info.pancancer.arch3.persistence;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.utils.Utilities;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class PostgreSQLIT {
    private File configFile;
    private PostgreSQL postgres;

    @BeforeClass
    public static void setup() throws IOException {
        Utilities.clearState();
    }

    @Before
    public void setUp() throws IOException {
        this.configFile = FileUtils.getFile("src", "test", "resources", "config.json");
        Utilities u = new Utilities();
        JSONObject parseConfig = u.parseConfig(configFile.getAbsolutePath());
        this.postgres = new PostgreSQL(parseConfig);

        // clean up the database
        postgres.clearDatabase();
    }

    /**
     * Test of getPendingProvisionUUID method, of class PostgreSQL.
     */
    @Test
    public void testGetPendingProvisionUUID() {
        Provision p = createProvision();
        p.setState(ProvisionState.PENDING);
        String uuid = postgres.createProvision(p);
        String result = postgres.getPendingProvisionUUID();
        assertEquals(uuid, result);
    }

    /**
     * Test of updatePendingProvision method, of class PostgreSQL.
     */
    @Test
    public void testUpdatePendingProvision() {
        Provision p = createProvision();
        p.setState(ProvisionState.PENDING);
        String uuid = postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.RUNNING);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.updatePendingProvision(uuid);
        result = postgres.getProvisionCount(ProvisionState.RUNNING);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of finishContainer method, of class PostgreSQL.
     */
    @Test
    public void testFinishContainer() {
        Provision p = createProvision();
        p.setState(ProvisionState.PENDING);
        String uuid = postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.finishContainer(uuid);
        result = postgres.getProvisionCount(ProvisionState.SUCCESS);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of finishJob method, of class PostgreSQL.
     */
    @Test
    public void testFinishJob() {
        postgres.createJob(createJob());
        postgres.createJob(createJob());
        // create one job with a defined state
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        String uuid = postgres.createJob(createJob);
        // get everything
        List<Job> jobs = postgres.getJobs(null);
        Assert.assertTrue("found jobs, incorrect number" + jobs.size(), jobs.size() == 3);
        List<Job> jobs2 = postgres.getJobs(JobState.SUCCESS);
        Assert.assertTrue("found jobs, incorrect number" + jobs2.size(), jobs2.isEmpty());
        postgres.finishJob(uuid);
        List<Job> jobs3 = postgres.getJobs(JobState.SUCCESS);
        Assert.assertTrue("found jobs, incorrect number" + jobs3.size(), jobs3.size() == 1);
    }

    /**
     * Test of updateJob method, of class PostgreSQL.
     */
    @Test
    public void testUpdateJob() {
        postgres.createJob(createJob());
        postgres.createJob(createJob());
        // create one job with a defined state
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        String uuid = postgres.createJob(createJob);
        // get everything
        List<Job> jobs = postgres.getJobs(null);
        Assert.assertTrue("found jobs, incorrect number" + jobs.size(), jobs.size() == 3);
        List<Job> jobs2 = postgres.getJobs(JobState.PENDING);
        Assert.assertTrue("found jobs, incorrect number" + jobs2.size(), jobs2.size() == 1);
        postgres.updateJob(uuid, "none", JobState.FAILED);
        List<Job> jobs3 = postgres.getJobs(JobState.PENDING);
        Assert.assertTrue("found jobs, incorrect number" + jobs3.size(), jobs3.isEmpty());
    }

    /**
     * Test of updateProvision method, of class PostgreSQL.
     */
    @Test
    public void testUpdateProvision() {
        Provision p = createProvision();
        p.setState(ProvisionState.PENDING);
        String uuid = postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.updateProvision(uuid, "job_uuid", ProvisionState.FAILED);
        result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of getProvisionCount method, of class PostgreSQL.
     */
    @Test
    public void testGetProvisionCount() {
        Provision p = createProvision();
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.PENDING);
        Assert.assertTrue("could not count provisions " + result, result == 1);
    }

    /**
     * Test of createProvision method, of class PostgreSQL.
     */
    @Test
    public void testCreateProvision() {
        Provision p = createProvision();
        String result = postgres.createProvision(p);
        Assert.assertTrue("could not create provision " + p.toJSON(), !result.isEmpty());
    }

    public Provision createProvision() {
        int cores = Base.DEFAULT_NUM_CORES;
        int memGb = Base.DEFAULT_MEMORY;
        int storageGb = Base.DEFAULT_DISKSPACE;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");
        Provision p = new Provision(cores, memGb, storageGb, a);
        return p;
    }

    public Job createJob() {
        String uuid = UUID.randomUUID().toString().toLowerCase();
        Utilities u = new Utilities();
        String hashStr = u.digest(uuid);
        HashMap<String, String> hm = new HashMap<>();
        hm.put("param1", "bar");
        hm.put("param2", "foo");
        return new Job("DEWrapperWorkflow", "1.0.0", "/path/to/workflow", hashStr, hm);
    }

    /**
     * Test of createJob method, of class PostgreSQL.
     */
    @Test
    public void testCreateJob() {
        Job j = createJob();
        String result = postgres.createJob(j);
        Assert.assertTrue("could not create job " + j.toJSON(), !result.isEmpty());
    }

    /**
     * Test of getJobs method, of class PostgreSQL.
     */
    @Test
    public void testGetJobs() {
        postgres.createJob(createJob());
        postgres.createJob(createJob());
        // create one job with a defined state
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        postgres.createJob(createJob);
        // get everything
        List<Job> jobs = postgres.getJobs(null);
        Assert.assertTrue("found jobs, incorrect number" + jobs.size(), jobs.size() == 3);
        List<Job> jobs2 = postgres.getJobs(JobState.PENDING);
        Assert.assertTrue("found jobs, incorrect number" + jobs2.size(), jobs2.size() == 1);

    }

    /**
     * Test of previouslyRun method, of class PostgreSQL.
     */
    @Test
    public void testPreviouslyRun() {
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        createJob.setJobHash("test_hash");
        postgres.createJob(createJob);
        boolean result = postgres.previouslyRun("test_hash");
        assertEquals(true, result);
    }

}
