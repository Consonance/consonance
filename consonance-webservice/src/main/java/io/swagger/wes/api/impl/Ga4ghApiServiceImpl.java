package io.swagger.wes.api.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;

import io.consonance.arch.beans.JobState;
import io.consonance.client.WebClient;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.consonance.common.Utilities;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.client.ApiException;
import io.swagger.client.api.OrderApi;

import io.swagger.models.auth.In;
import io.swagger.wes.api.*;
import io.swagger.wes.model.*;

import io.consonance.arch.beans.Job;

import io.swagger.wes.model.Ga4ghWesServiceInfo;
import io.swagger.wes.model.Ga4ghWesWorkflowListResponse;
import io.swagger.wes.model.Ga4ghWesWorkflowLog;
import io.swagger.wes.model.Ga4ghWesWorkflowRequest;
import io.swagger.wes.model.Ga4ghWesWorkflowRunId;
import io.swagger.wes.model.Ga4ghWesWorkflowStatus;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import io.swagger.wes.api.NotFoundException;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.resources.OrderResource;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.validator.routines.UrlValidator;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-09-15T17:06:31.319-07:00")
public class Ga4ghApiServiceImpl extends Ga4ghApiService {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Ga4ghApiServiceImpl.class);

    private static ConsonanceWebserviceConfiguration config;
    private static OrderResource orderResource;
    private static Gson gson = new Gson();

    public static void setConfig(ConsonanceWebserviceConfiguration config) {
        Ga4ghApiServiceImpl.config = config;
    }

    public static void setOrderResource(OrderResource orderResource) {
        Ga4ghApiServiceImpl.orderResource = orderResource;
    }

    @Override
    public Response cancelJob(String workflowId, ConsonanceUser user) throws NotFoundException {
        LOG.info("Hit WES API! Called Ga4ghApiServiceImpl.cancelJob()");
        LOG.info("Cancelling job " + workflowId);

//        final Job workflowRun = orderResource.getWorkflowRun(user, workflowId);
        // no-op, Consonance doesn't really support cancellation
        Ga4ghWesWorkflowRunId id = new Ga4ghWesWorkflowRunId();
//        id.setWorkflowId(workflowRun.getUuid());
        return Response.ok().entity(workflowId).build();
    }

    /**
     * Provides information on the service, versions ...
     * */
    @Override
    public Response getServiceInfo(ConsonanceUser user) throws NotFoundException {
        LOG.info("Hit WES API! Called Ga4ghApiServiceImpl.getServiceInfo()");
        Ga4ghWesServiceInfo serviceInfo = new Ga4ghWesServiceInfo();
        // TODO: Query DB to obtain this values.
        // Supported formats
        String cwl = "cwl";
        String wdl = "wdl";
        // Version of each
        String wdlVersion = "1.0"; // Latest release 2017-08-16T00:00:00.000-00:00
        String cwlVersion = "1.0";

        // Supported WDL version
        Ga4ghWesWorkflowTypeVersion versionTypeWDL = new Ga4ghWesWorkflowTypeVersion();
        versionTypeWDL.addWorkflowTypeVersionItem(wdlVersion);
        // Add to Map the WDL specifics
        serviceInfo.putWorkflowTypeVersionsItem(wdl, versionTypeWDL);

        // Supported CWL version
        Ga4ghWesWorkflowTypeVersion versionTypeCWL = new Ga4ghWesWorkflowTypeVersion();
        versionTypeCWL.addWorkflowTypeVersionItem(cwlVersion);
        // Add it to the map
        serviceInfo.putWorkflowTypeVersionsItem(cwl, versionTypeCWL);

        // WES version supported
        String wesApiVersion = "v1.0";
        // Add to map
        serviceInfo.addSupportedWesVersionsItem(wesApiVersion);

        // Supported file system protocol
        List<String> supportedFileProtocols = Arrays.asList("http", "https", "sftp", "s3", "gs", "file", "synapse");
        // Add to map
        serviceInfo.setSupportedFilesystemProtocols(supportedFileProtocols);

        // Engine and version
        String cwltool = "cwltool";
        String cwltoolVersion = "1.0.20171107133715";
        serviceInfo.putEngineVersionsItem(cwltool, cwltoolVersion);
        String dockstore = "dockstore";
        String dockstoreVersion = "1.3.0";
        serviceInfo.putEngineVersionsItem(dockstore, dockstoreVersion);

        // System state counts
        // TODO: cordinate this message for endpoint, each state requires to be acknowledge
        serviceInfo.putSystemStateCountsItem("Unknown", (long) 0);
        serviceInfo.putSystemStateCountsItem("Queued", (long) 0);
        serviceInfo.putSystemStateCountsItem("Running", (long) 0);
        serviceInfo.putSystemStateCountsItem("Paused", (long) 0);
        serviceInfo.putSystemStateCountsItem("Complete", (long) 0);
        serviceInfo.putSystemStateCountsItem("Error", (long) 0);
        serviceInfo.putSystemStateCountsItem("SystemError", (long) 0);
        serviceInfo.putSystemStateCountsItem("Canceled", (long) 0);
        serviceInfo.putSystemStateCountsItem("Initializing", (long) 0);

        //TODO: properly design the report-back metadata parametes.
        serviceInfo.putKeyValuesItem("Metadata", "Nothing to report");


        return Response.ok().entity(serviceInfo).build();

    }

    /**
     * Ultimo ;)
     */
    @Override
    public Response getWorkflowLog(String workflowId, ConsonanceUser user) throws NotFoundException {
//        LOG.info("Hit WES API! Called Ga4ghApiServiceImpl.getWorkflowLog()");


        // Initializing structure
        Ga4ghWesWorkflowLog log = new Ga4ghWesWorkflowLog();
        log.setWorkflowId(workflowId);

//        WebClient client = getClient();
//        OrderApi jobApi = new OrderApi(client);

        final String[] jobUuid = {"-1"};

        // Changing return typt to models [ arch.beans to -> client.model]
        List<Job> allJobs = orderResource.listWorkflowRuns(user);
        allJobs.stream().filter((Job t) -> String.valueOf(t.getJobId()).equals(workflowId)).forEach(s -> jobUuid[0] = s.getUuid());

        LOG.info(orderResource.getWorkflowRun(user, jobUuid[0]).toString());
        Job parseJob = orderResource.getWorkflowRun(user, jobUuid[0]);

        // Instantiate request
        Ga4ghWesWorkflowRequest request = new Ga4ghWesWorkflowRequest();

        // Parse response
        request.setWorkflowDescriptor(parseJob.getContainerImageDescriptor());
        request.setWorkflowParams(parseJob.getContainerRuntimeDescriptor());
        request.setWorkflowType(parseJob.getContainerImageDescriptorType());
        request.setWorkflowTypeVersion("1.0");

        // Set request to response
        log.setRequest(request);

        // Set Job State
        log.setState(mapState(parseJob.getState()));

        // Instantiate Log
        Ga4ghWesLog workflowLog = new Ga4ghWesLog();
        // Parse job object to ga4gh log object, hardcode undefined parameters.
        workflowLog.setName("CWL|WDL Job");
        workflowLog.addCmdItem("run workflow");
        workflowLog.setStartTime(String.valueOf(parseJob.getCreateTimestamp()));
        workflowLog.setEndTime(String.valueOf(parseJob.getUpdateTimestamp()));
        workflowLog.setStdout(parseJob.getStdout());
        workflowLog.setStderr(parseJob.getStderr());
        workflowLog.setExitCode(0);

        // Add instance to log object
        log.setWorkflowLog(workflowLog);

        // Iterative method across the whole registry/ update constantly. //TODO: Implement methos accross system.
        log.addTaskLogsItem(workflowLog);

        Ga4ghWesParameter outputParameter = new Ga4ghWesParameter();

        // Nothing to pull out. //TODO: Implement methos correctly across consonance.
        log.addOutputsItem(outputParameter);


        LOG.info(log.toString());
        return Response.ok().entity(log).build();
    }

    @Override
    public Response getWorkflowStatus(String workflowId, ConsonanceUser user) throws NotFoundException {
//        LOG.info("Hit WES API! Called Ga4ghApiServiceImpl.getWorkflowStatus()");
//        WebClient client = getClient();
//        OrderApi jobApi = new OrderApi(client);

        Ga4ghWesWorkflowStatus workflowStatus = new Ga4ghWesWorkflowStatus();

        List<Job> allJobs = orderResource.listWorkflowRuns(user);
        allJobs.stream().filter((Job t) -> String.valueOf(t.getJobId()).equals(workflowId)).forEach(
                s -> {
                    workflowStatus.setState(mapState(s.getState()));
                    workflowStatus.setWorkflowId(String.valueOf(s.getJobId()));
                });

        LOG.info(workflowStatus.toString());

        return Response.ok().entity(workflowStatus).build();
    }

    /***
     * If there is jobs in the queue, and running.
     * Listing workflows will show their Id, and Status.
     */

    @Override
    public Response listWorkflows(Long pageSize, String pageToken, String keyValueSearch, ConsonanceUser user) throws NotFoundException {
//        LOG.info("Hit WES API! Called Ga4ghApiServiceImpl.listWorkflows()");
//        LOG.info("Listing workflows with page size " + pageSize + ", pageToken=" + pageToken + ", and keyValueSearch=" + keyValueSearch);

        Long optionalPageSize = pageSize;
        String optionalPageToken = pageToken;
        String optionalKeyFilter = keyValueSearch;


        Ga4ghWesWorkflowListResponse list = new Ga4ghWesWorkflowListResponse();

//        WebClient client = getClient();
//        OrderApi jobApi = new OrderApi(client);

        List<Job> allJobs = orderResource.listWorkflowRuns(user);
        allJobs.stream().map((Job t) -> {
                    Ga4ghWesWorkflowDesc descriptor = new Ga4ghWesWorkflowDesc();
                    descriptor.setWorkflowId(String.valueOf(t.getJobId()));
                    descriptor.setState(mapState(t.getState()));
                    return descriptor;
        }).forEach(e -> list.addWorkflowsItem(e)); // Correct call integrate with GA4GH protocols

        // Authenticate ConsonanceUser
        if (user != null) {
            // TODO: Autheticate, refer to DB and confirm refgister user.

        }
        LOG.info(list.toString());
        return Response.ok().entity(list).build();
    }

    /**
     * Calls on existing method to schedule a job.
     * Requires authenticate user (DB?)
     */
    @Override
    public Response runWorkflow(Ga4ghWesWorkflowRequest body, ConsonanceUser user) throws NotFoundException, IOException, TimeoutException {
//        LOG.info("Hit WES API! Called Ga4ghApiServiceImpl.runWorkflow()");
        LOG.info(body.toString());

        // TODO: Check if version in body, matches consonance version. Confirm non-empty value.
        String workflowTypeVersion = body.getWorkflowTypeVersion(); // Version of runner tool.
        if (workflowTypeVersion.isEmpty()) {
            return Response.noContent().build();
        } else {
            String currentRunnerVersion = "RandomVersionTEMP";
        }

        // Process deserialize body. TODO: Confirm non-empty values.
        String workflowDescriptor = body.getWorkflowDescriptor(); // Runner descriptor, < cwl || wdl> file.
        if (workflowDescriptor.isEmpty()) {
            return Response.noContent().build();
        }

        String workflowParams = body.getWorkflowParams(); // Runtime descriptor, <json> file.
        if (workflowParams.isEmpty()) {
            return Response.noContent().build();
        }

        String workflowType = body.getWorkflowType(); // <CWL | WDL> param.
        if (workflowType.isEmpty()) {
            return Response.noContent().build();
        }

        // Optional parameters.
        Map workflowKeyValues = body.getKeyValues();


        // Authenticate ConsonanceUser
        if (user != null) {
            // TODO: Autheticate, refer to DB and confirm refgister user.

        }

//        // TODO: Might not require to build this whole chunk, Job was incorrect. Correct and try again.
//        Boolean correctUser = true;
//        File configFile = FileUtils.getFile("src", "test", "resources", "config");
//        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
//        WebClient client = new WebClient();
//        client.setBasePath(parseConfig.getString(Constants.WEBSERVICE_BASE_PATH));
//        client.addDefaultHeader("Authorization", "Bearer " + (correctUser ? parseConfig.getString(Constants.WEBSERVICE_TOKEN) : "foobar"));

        // Do something with client.
//        OrderApi jobApi = new OrderApi(client);

        Ga4ghWesWorkflowRunId runId = new Ga4ghWesWorkflowRunId();

        final Job newJob = new Job();
        newJob.setContainerRuntimeDescriptor(workflowParams);
        newJob.setContainerImageDescriptor(workflowDescriptor);
        newJob.setContainerImageDescriptorType(workflowType);

        orderResource.addOrder(user, newJob);
        runId.setWorkflowId(String.valueOf(orderResource.listOwnedWorkflowRuns(user).get(0).getJobId()));
        LOG.info(runId.toString());
        return Response.ok().entity(runId).build();

    }

    private Ga4ghWesState mapState(JobState state) {

        Ga4ghWesState ga4ghState = Ga4ghWesState.UNKNOWN;

        switch (state){
            case LOST:
                ga4ghState = Ga4ghWesState.SYSTEMERROR;

            case START:
                ga4ghState = Ga4ghWesState.INITIALIZING;

            case FAILED:
                ga4ghState = Ga4ghWesState.ERROR;

            case SUCCESS:
                ga4ghState = Ga4ghWesState.COMPLETE;

            case RUNNING:
                ga4ghState = Ga4ghWesState.RUNNING;

            case PENDING:
                ga4ghState = Ga4ghWesState.QUEUED;

        }

        return ga4ghState;
    }

//    private WebClient getClient(){
//        Boolean correctUser = true;
//        File configFile = FileUtils.getFile("src", "test", "resources", "config");
//        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
//        WebClient client = new WebClient();
//        client.setBasePath(parseConfig.getString(Constants.WEBSERVICE_BASE_PATH));
//        client.addDefaultHeader("Authorization", "Bearer " + (correctUser ? parseConfig.getString(Constants.WEBSERVICE_TOKEN) : "foobar"));
//
//        return client;
//    }
}
