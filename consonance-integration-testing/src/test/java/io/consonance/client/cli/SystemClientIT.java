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

package io.consonance.client.cli;

import io.consonance.arch.beans.JobState;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.client.WebClient;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.consonance.common.Utilities;
import io.consonance.webservice.ConsonanceWebserviceApplication;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.swagger.client.ApiException;
import io.swagger.client.api.OrderApi;
import io.swagger.client.api.UserApi;
import io.swagger.client.model.ConsonanceUser;
import io.swagger.client.model.Job;
import io.swagger.wes.api.Ga4ghApi;
import io.swagger.wes.api.NotFoundException;
import io.swagger.wes.model.Ga4ghWesServiceInfo;
import io.swagger.wes.model.Ga4ghWesWorkflowListResponse;
import io.swagger.wes.model.Ga4ghWesWorkflowRequest;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test only the swagger-extended webclient.
 *
 * @author dyuen
 */
public class SystemClientIT {

    @ClassRule
    public static final DropwizardAppRule<ConsonanceWebserviceConfiguration> RULE =
            new DropwizardAppRule<>(ConsonanceWebserviceApplication.class, ResourceHelpers.resourceFilePath("run-fox.yml"));

    public static WebClient getWebClient() throws IOException, TimeoutException {
        return getWebClient(true);
    }

    public static WebClient getWebClient(boolean correctUser) throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        WebClient client = new WebClient();
        client.setBasePath(parseConfig.getString(Constants.WEBSERVICE_BASE_PATH));
        client.addDefaultHeader("Authorization", "Bearer " + (correctUser? parseConfig.getString(Constants.WEBSERVICE_TOKEN) : "foobar"));
        return client;
    }

    @Test(expected = ApiException.class)
    public void testListUsersWithoutAuthentication() throws IOException, TimeoutException, ApiException {
        WebClient client = getWebClient(false);
        UserApi userApi = new UserApi(client);
        final List<ConsonanceUser> consonanceUsers = userApi.listUsers();
        // should just be the one admin user after we clear it out
        assertThat(consonanceUsers.size() > 1);
    }


    @Test
    public void testListUsers() throws ApiException, IOException, TimeoutException {
        WebClient client = getWebClient();
        UserApi userApi = new UserApi(client);
        final List<ConsonanceUser> consonanceUsers = userApi.listUsers();
        // should just be the one admin user after we clear it out
        assertThat(consonanceUsers.size() > 1);
    }

    @Test
    public void testScheduleAndListJobs() throws ApiException, IOException, TimeoutException {
        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);
        List<Job> allJobs = jobApi.listWorkflowRuns();
        List<Job> myJobs = jobApi.listOwnedWorkflowRuns();
        assertThat(allJobs.size() == 0 && myJobs.size() == 0);
        // schedule a job for myself via the api
        final Job clientJob = createClientJob();
        jobApi.addOrder(clientJob);
        allJobs = jobApi.listWorkflowRuns();
        myJobs = jobApi.listOwnedWorkflowRuns();
        assertThat(myJobs.size() == 1 && allJobs.size() == 1);
        assertThat(myJobs.get(0).getEndUser().equals("admin@admin.com"));
    }

    @Test
    public void testStateAndStdoutChangeOnBackEnd() throws ApiException, IOException, TimeoutException {
        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);
        List<Job> allJobs = jobApi.listWorkflowRuns();
        List<Job> myJobs = jobApi.listOwnedWorkflowRuns();
        assertThat(allJobs.size() == 0 && myJobs.size() == 0);
        // schedule a job for myself via the api
        final Job clientJob = createClientJob();
        jobApi.addOrder(clientJob);
        Job jobFromServer =  jobApi.listOwnedWorkflowRuns().get(0);

        // state change using a direct DB connection
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        PostgreSQL postgres = new PostgreSQL(parseConfig);
        postgres.updateJob(jobFromServer.getJobUuid(), jobFromServer.getVmUuid(), JobState.FAILED);

        jobFromServer =  jobApi.listOwnedWorkflowRuns().get(0);
        assertThat(jobFromServer.getState() == Job.StateEnum.FAILED);

        // test stdout and stderr
        postgres.updateJobMessage(jobFromServer.getJobUuid(), "funky town stdout", "funky town stderr");
        jobFromServer =  jobApi.listOwnedWorkflowRuns().get(0);
        assertThat(jobFromServer.getStdout().equals("funky town stdout") && jobFromServer.getStderr().equals("funky town stderr"));
    }

    @Test
    public void testScheduleForSomeoneElse() throws ApiException, IOException, TimeoutException {
        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);
        List<Job> allJobs = jobApi.listWorkflowRuns();
        List<Job> myJobs = jobApi.listOwnedWorkflowRuns();
        assertThat(allJobs.size() == 0 && myJobs.size() == 0);
        // schedule a job for someone else using a direct DB connection
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        PostgreSQL postgres = new PostgreSQL(parseConfig);
        postgres.createJob(createServerJob());

        // job should not appear for me
        allJobs = jobApi.listWorkflowRuns();
        myJobs = jobApi.listOwnedWorkflowRuns();
        assertThat(allJobs.size() == 1 && myJobs.size() == 0);
    }

    private Job createClientJob() {
        final Job job = new Job();
        job.setJobUuid("42");
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
        job.setStdout("My god");
        job.setStderr("It's full of stars");
        job.setFlavour("m1.funky");
        return job;
    }

    private io.consonance.arch.beans.Job createServerJob() {
        final io.consonance.arch.beans.Job job = new io.consonance.arch.beans.Job();
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
        job.setStdout("My god");
        job.setStderr("It's full of stars");
        job.setFlavour("m1.funky");
        return job;
    }

}
