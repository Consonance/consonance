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
import java.util.List;
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

    @Ignore
    @Test
    public void testWesGetServiceInfo() throws NotFoundException {
        //TODO: Implementation ready, develop test.
        Ga4ghApi ga4ghApi = new Ga4ghApi();

        io.consonance.webservice.core.ConsonanceUser user = new io.consonance.webservice.core.ConsonanceUser();

        Response response = ga4ghApi.getServiceInfo(user);

        String assertion = "class Ga4ghWesServiceInfo {\n" +
                "    workflowTypeVersions: {cwl=class Ga4ghWesWorkflowTypeVersion {\n" +
                "        workflowTypeVersion: [1.0]\n" +
                "    }, wdl=class Ga4ghWesWorkflowTypeVersion {\n" +
                "        workflowTypeVersion: [1.0]\n" +
                "    }}\n" +
                "    supportedWesVersions: [v1.0]\n" +
                "    supportedFilesystemProtocols: [http, https, sftp, s3, gs, file, synapse]\n" +
                "    engineVersions: {cwltool=1.0.20171107133715, dockstore=1.3.0}\n" +
                "    systemStateCounts: {SystemError=0, Unknown=0, Complete=0, Running=0, Error=0, Queued=0, Canceled=0, Paused=0, Initializing=0}\n" +
                "    keyValues: {Metadata=Nothing to report}\n" +
                "}";


        Assert.assertEquals(response.getEntity().toString(),assertion);
    }
    // TODO: Fix test, test broken, no implementation of the GA4GH client yet.
    @Ignore
    @Test
    public void testWesRunWorkflow() throws NotFoundException, IOException, TimeoutException, ApiException {
        Ga4ghApi ga4ghApi = new Ga4ghApi();

        io.consonance.webservice.core.ConsonanceUser user = new io.consonance.webservice.core.ConsonanceUser();

        Ga4ghWesWorkflowRequest requestBody = new Ga4ghWesWorkflowRequest();

        Job testRunJob = createClientJob();

        requestBody.setWorkflowDescriptor(testRunJob.getContainerImageDescriptor());
        requestBody.setWorkflowParams(testRunJob.getContainerRuntimeDescriptor());
        requestBody.workflowType("cwl");
        requestBody.setWorkflowTypeVersion("1.0");


        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);
        List<Job> allJobs = jobApi.listWorkflowRuns();
        List<Job> myJobs = jobApi.listOwnedWorkflowRuns();
        assertThat(allJobs.size() == 0 && myJobs.size() == 0);
        Response response = ga4ghApi.runWorkflow(requestBody, user);

        allJobs = jobApi.listWorkflowRuns();
        myJobs = jobApi.listOwnedWorkflowRuns();

        assertThat(response.getEntity().toString() == "1");
        assertThat(myJobs.size() == 1 && allJobs.size() == 1);
//        assertThat(myJobs.get(0).getEndUser().equals("admin@admin.com"));

        // Update with another request. There should be 2 jobs running.
        requestBody.setWorkflowDescriptor(createServerJob().getContainerImageDescriptor());
        requestBody.setWorkflowParams(createServerJob().getContainerRuntimeDescriptor());
        response = ga4ghApi.runWorkflow(requestBody, user);

        allJobs = jobApi.listWorkflowRuns();
        myJobs = jobApi.listOwnedWorkflowRuns();

        assertThat(response.getEntity().toString() == "2");
        assertThat(myJobs.size() == 2 && allJobs.size() == 2);
    }

    // TODO: Fix test, test broken, no implementation of the GA4GH client yet.
    @Ignore
    @Test
    public void testListWorkflows() throws NotFoundException, IOException, TimeoutException, ApiException{
        Ga4ghApi ga4ghApi = new Ga4ghApi();

        io.consonance.webservice.core.ConsonanceUser user = new io.consonance.webservice.core.ConsonanceUser();

        Ga4ghWesWorkflowRequest requestBody = new Ga4ghWesWorkflowRequest();

        Job testRunJob = createClientJob();

        requestBody.setWorkflowDescriptor(testRunJob.getContainerImageDescriptor());
        requestBody.setWorkflowParams(testRunJob.getContainerRuntimeDescriptor());
        requestBody.workflowType("cwl");
        requestBody.setWorkflowTypeVersion("1.0");


        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);

        ga4ghApi.runWorkflow(requestBody, user);

        Ga4ghWesWorkflowRequest requestBody2 = new Ga4ghWesWorkflowRequest();
        requestBody2.setWorkflowDescriptor(testRunJob.getContainerImageDescriptor());
        requestBody2.setWorkflowParams(testRunJob.getContainerRuntimeDescriptor());
        requestBody2.workflowType("cwl");
        requestBody2.setWorkflowTypeVersion("1.0");
        // Update with another request. There should be 2 jobs running.
        requestBody2.setWorkflowDescriptor(createServerJob().getContainerImageDescriptor());
        requestBody2.setWorkflowParams(createServerJob().getContainerRuntimeDescriptor());
        ga4ghApi.runWorkflow(requestBody2, user);

        Response listWorkflows = ga4ghApi.listWorkflows(Long.valueOf(0), "0","", user);

        Ga4ghWesWorkflowListResponse listResponse = (Ga4ghWesWorkflowListResponse) listWorkflows.getEntity();
        Assert.assertEquals(2, listResponse.getWorkflows().size());
    }

    @Ignore
    @Test
    public void testGetWorkflowLog() throws IOException, TimeoutException, NotFoundException {
        Ga4ghApi ga4ghApi = new Ga4ghApi();

        io.consonance.webservice.core.ConsonanceUser user = new io.consonance.webservice.core.ConsonanceUser();

        Ga4ghWesWorkflowRequest requestBody = new Ga4ghWesWorkflowRequest();

        Job testRunJob = createClientJob();

        requestBody.setWorkflowDescriptor(testRunJob.getContainerImageDescriptor());
        requestBody.setWorkflowParams(testRunJob.getContainerRuntimeDescriptor());
        requestBody.workflowType("cwl");
        requestBody.setWorkflowTypeVersion("1.0");


        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);

        ga4ghApi.runWorkflow(requestBody, user);

        Response workflowLogs = ga4ghApi.getWorkflowLog("1", user);


        assertThat(getLogs().equals(workflowLogs.getEntity().toString()));
    }
    @Ignore
    @Test
    public void testGetWorkflowStatus() throws IOException, TimeoutException, NotFoundException {
        Ga4ghApi ga4ghApi = new Ga4ghApi();

        io.consonance.webservice.core.ConsonanceUser user = new io.consonance.webservice.core.ConsonanceUser();

        Ga4ghWesWorkflowRequest requestBody = new Ga4ghWesWorkflowRequest();

        Job testRunJob = createClientJob();

        requestBody.setWorkflowDescriptor(testRunJob.getContainerImageDescriptor());
        requestBody.setWorkflowParams(testRunJob.getContainerRuntimeDescriptor());
        requestBody.workflowType("cwl");
        requestBody.setWorkflowTypeVersion("1.0");


        WebClient client = getWebClient();
        OrderApi jobApi = new OrderApi(client);

        ga4ghApi.runWorkflow(requestBody, user);

        Response workflowLogs = ga4ghApi.getWorkflowStatus("1", user);

        assertThat(getStatus().equals(workflowLogs));

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

    private String getStatus(){
        return "class Ga4ghWesWorkflowStatus {\n" +
                "    workflowId: 1\n" +
                "    state: Queued\n" +
                "}";
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

    private String getLogs(){
        return "class Ga4ghWesWorkflowLog {\n" +
                "    workflowId: 1\n" +
                "    request: class Ga4ghWesWorkflowRequest {\n" +
                "        workflowDescriptor: {\n" +
                "        \n" +
                "            \"items\": [\n" +
                "                {\n" +
                "                    \"index\": 1,\n" +
                "                    \"index_start_at\": 56,\n" +
                "                    \"integer\": 19,\n" +
                "                    \"float\": 15.1507,\n" +
                "                    \"name\": \"Ashley\",\n" +
                "                    \"surname\": \"Coley\",\n" +
                "                    \"fullname\": \"Brenda Raynor\",\n" +
                "                    \"email\": \"anita@poole.sy\",\n" +
                "                    \"bool\": true\n" +
                "                }\n" +
                "            ]\n" +
                "        \n" +
                "        }\n" +
                "        workflowParams: {\n" +
                "        \n" +
                "            \"items\": [\n" +
                "                {\n" +
                "                    \"index\": 1,\n" +
                "                    \"index_start_at\": 56,\n" +
                "                    \"integer\": 1,\n" +
                "                    \"float\": 18.5884,\n" +
                "                    \"name\": \"Lee\",\n" +
                "                    \"surname\": \"Summers\",\n" +
                "                    \"fullname\": \"Sandra Alexander\",\n" +
                "                    \"email\": \"ronnie@byrne.gh\",\n" +
                "                    \"bool\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        \n" +
                "        }\n" +
                "        workflowType: cwl\n" +
                "        workflowTypeVersion: 1.0\n" +
                "        keyValues: null\n" +
                "    }\n" +
                "    state: Queued\n" +
                "    workflowLog: class Ga4ghWesLog {\n" +
                "        name: CWL|WDL Job\n" +
                "        cmd: [run workflow]\n" +
                "        startTime: Tue Dec 19 16:47:50 PST 2017\n" +
                "        endTime: Tue Dec 19 16:47:50 PST 2017\n" +
                "        stdout: null\n" +
                "        stderr: null\n" +
                "        exitCode: 0\n" +
                "    }\n" +
                "    taskLogs: [class Ga4ghWesLog {\n" +
                "        name: CWL|WDL Job\n" +
                "        cmd: [run workflow]\n" +
                "        startTime: Tue Dec 19 16:47:50 PST 2017\n" +
                "        endTime: Tue Dec 19 16:47:50 PST 2017\n" +
                "        stdout: null\n" +
                "        stderr: null\n" +
                "        exitCode: 0\n" +
                "    }]\n" +
                "    outputs: [class Ga4ghWesParameter {\n" +
                "        name: null\n" +
                "        value: null\n" +
                "        location: null\n" +
                "        type: null\n" +
                "    }]\n" +
                "}";
    }
}
