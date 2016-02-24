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

package io.consonance.common;

import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.beans.ProvisionState;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.arch.reporting.Arch3ReportImpl;
import io.consonance.arch.reporting.ReportAPI;
import io.consonance.arch.reporting.SlackRenderer;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

/**
 * @author dyuen
 */
public class Arch3ReportImplTest {

    private Arch3ReportImpl arch3Impl;
    private PostgreSQL postgres;

    @Before
    public void setup() throws IOException, TimeoutException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = CommonTestUtilities.parseConfig(configFile.getAbsolutePath());
        this.postgres = new PostgreSQL(parseConfig);
        // clean up the database
        postgres.clearDatabase();
        this.arch3Impl = new Arch3ReportImpl(parseConfig,postgres);
    }

    @Test
    public void testGetVMStateCounts() throws Exception {
        // create some VMs and their state
        Provision p = new Provision();
        p.setState(ProvisionState.RUNNING);
        postgres.createProvision(p);
        postgres.createProvision(p);
        p.setState(ProvisionState.FAILED);
        postgres.createProvision(p);
        assertTrue(arch3Impl.getVMStateCounts().size() == 2);
    }

    @Test
    public void testGetJobStateCounts() throws Exception {
        // create some VMs and their state
        Job j = new Job();
        j.setState(JobState.FAILED);
        postgres.createJob(j);
        postgres.createJob(j);
        j.setState(JobState.LOST);
        postgres.createJob(j);
        assertTrue(arch3Impl.getJobStateCounts().size() == 2);
    }


    @Test
    public void testGetCommands() throws Exception {
        assertTrue(arch3Impl.getCommands().size() > 0);
    }

    @Test
    public void testGetJobInfo() throws Exception {
        Job j = new Job();
        j.setState(JobState.FAILED);
        Job k = new Job();
        k.setState(JobState.FAILED);
        Job l = new Job();
        l.setState(JobState.FAILED);
        postgres.createJob(j);
        postgres.createJob(k);
        postgres.createJob(l);
        postgres.runUpdateStatement("update job set create_timestamp=NOW()");
        postgres.runUpdateStatement("update job set update_timestamp=NOW()");
        // set times, this appears broken
        assertTrue(arch3Impl.getJobInfo().size() == 3);
    }

    @Test
    public void testSlackRendering(){
        Job j = new Job();
        j.setState(JobState.FAILED);
        postgres.createJob(j);
        postgres.runUpdateStatement("update job set create_timestamp=NOW()");
        postgres.runUpdateStatement("update job set update_timestamp=NOW()");
        SlackRenderer render = new SlackRenderer(arch3Impl);
        final SlackRenderer.FormattedMessage formattedMessage = render.convertToResult(ReportAPI.Commands.JOBS.toString().toUpperCase());
        assertTrue(formattedMessage.attachment.toString().contains(JobState.FAILED.toString()));
    }
}
