package io.swagger.wes.api.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import io.consonance.arch.Base;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.client.WebClient;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.consonance.common.Utilities;
import io.dockstore.client.cli.Client;
import io.dockstore.client.cli.nested.AbstractEntryClient;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.client.ApiException;
import io.swagger.client.api.OrderApi;

import io.swagger.client.model.ExtraFile;
import io.swagger.client.model.SourceFile;
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

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import io.swagger.wes.api.NotFoundException;

import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import static java.nio.file.StandardOpenOption.*;



import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
import scala.Int;

import static io.dockstore.client.cli.ArgumentUtility.kill;

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

    private HierarchicalINIConfiguration settings = null;
    private static ConsonanceWebserviceConfiguration config;
    private static OrderResource orderResource;
    private static Gson gson = new Gson();

    public static void setConfig(ConsonanceWebserviceConfiguration config) {
        Ga4ghApiServiceImpl.config = config;
    }

    public static void setOrderResource(OrderResource orderResource) {
        Ga4ghApiServiceImpl.orderResource = orderResource;
    }

    // Cancels a running workflow by providing the id of the workflow
    @Override
    public Response cancelJob(String workflowId, ConsonanceUser user) throws NotFoundException {

        return Response.ok().build();
    }


    //Provides information on the service, versions ...
    @Override
    public Response getServiceInfo(ConsonanceUser user) throws NotFoundException {

        Ga4ghWesServiceInfo serviceInfo = new Ga4ghWesServiceInfo();
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
        List<String> supportedFileProtocols = Arrays.asList("http", "https", "sftp", "s3", "gs", "synapse");

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
        Map <JobState, Long> systemState = new HashMap<JobState, Long>();

        List<Job> allJobs = orderResource.listWorkflowRuns(user);
        if (allJobs.isEmpty()){
            serviceInfo.putSystemStateCountsItem("Unknown", (long) 0);
        }

        for (Job jobInList : allJobs){
            try{
                Long updatedValue = systemState.get(jobInList.getState())+((long) 1) ;
                systemState.put(jobInList.getState(), updatedValue);
            }catch (Exception e){
                systemState.put(jobInList.getState(), (long) 1);
            }
        }

        for (JobState key : systemState.keySet()){
            Ga4ghWesState mappedValue = mapState(key);
            serviceInfo.putSystemStateCountsItem(mappedValue.toString(), systemState.get(key));
        }

        serviceInfo.putKeyValuesItem("flavour", "e.g. (r2.medium), instance type descriptor for AWS");


        return Response.ok().entity(serviceInfo).build();

    }

    // Returns logs of a given workflow, by id
    @Override
    public Response getWorkflowLog(String workflowId, ConsonanceUser user) throws NotFoundException {

        // Initializing structure
        Ga4ghWesWorkflowLog log = new Ga4ghWesWorkflowLog();
        log.setWorkflowId(workflowId);

        final String[] jobUuid = {"-1"};


        List<Job> allJobs = orderResource.listWorkflowRuns(user);
        allJobs.stream().filter((Job t) -> String.valueOf(t.getJobId()).equals(workflowId)).forEach(s -> jobUuid[0] = s.getUuid());

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
        JobState workflowState = parseJob.getState();
        log.setState(mapState(workflowState));

        // Instantiate Log
        Ga4ghWesLog workflowLog = new Ga4ghWesLog();

        // Parse job object to ga4gh log object, hardcode undefined parameters.
        workflowLog.setName(parseJob.getContainerImageDescriptorType());
        workflowLog.setStartTime(String.valueOf(parseJob.getCreateTimestamp()));
        workflowLog.setEndTime(String.valueOf(parseJob.getUpdateTimestamp()));
        workflowLog.setStdout(parseJob.getStdout());
        workflowLog.setStderr(parseJob.getStderr());

        // Iterative method across the whole registry/ update constantly.
        log.addTaskLogsItem(workflowLog);

        Ga4ghWesParameter outputParameter = new Ga4ghWesParameter();

        // Nothing to pull out.
        log.addOutputsItem(outputParameter);

        return Response.ok().entity(log).build();
    }

    // Get the specific status of a workflow, i.e. STARTING, RUNNING, QUEUED... etc
    @Override
    public Response getWorkflowStatus(String workflowId, ConsonanceUser user) throws NotFoundException {

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

    // If there is jobs in the queue, and running.
    // Listing workflows will show their Id, and Status.
    @Override
    public Response listWorkflows(Long pageSize, String pageToken, String keyValueSearch, ConsonanceUser user) throws NotFoundException {
        Long optionalPageSize = pageSize;
        String optionalPageToken = pageToken;
        String optionalKeyFilter = keyValueSearch;

        Ga4ghWesWorkflowListResponse list = new Ga4ghWesWorkflowListResponse();

        List<Job> allJobs = orderResource.listWorkflowRuns(user);
        Map <Integer, JobState> serialized = new TreeMap<Integer, JobState>();

        allJobs.stream().forEach((Job j) -> serialized.put(j.getJobId(), j.getState()));

        for (Integer key: serialized.keySet()){
            Ga4ghWesWorkflowDesc descriptor = new Ga4ghWesWorkflowDesc();
            descriptor.setWorkflowId(String.valueOf(key));
            descriptor.setState(mapState(serialized.get(key)));
            list.addWorkflowsItem(descriptor);
        }

        return Response.ok().entity(list).build();
    }


     //Calls on existing method to schedule a job.
     //Requires user with the request to be authenticated
    @Override
    public Response runWorkflow(Ga4ghWesWorkflowRequest body, ConsonanceUser user) throws NotFoundException, IOException, TimeoutException {

        Map workflowKeyValues = body.getKeyValues();


        String workflowTypeVersion = body.getWorkflowTypeVersion(); // Version of runner tool.
        if (workflowTypeVersion.isEmpty()) {
            return Response.noContent().build();
        }

        final Job newJob = new Job();

        // Process deserialize body.
        UrlValidator urlValidator = new UrlValidator();
        String workflowDescriptor = body.getWorkflowDescriptor(); // Runner descriptor, < cwl || wdl> file.
        if (workflowDescriptor.isEmpty()) {
            return Response.noContent().build();
        }
        else if (urlValidator.isValid(workflowDescriptor)){ // A descriptor file hosted in the web.
            URL jobURL = new URL(workflowDescriptor);
            final Path tempFile = Files.createTempFile("image", "cwl");
            FileUtils.copyURLToFile(jobURL, tempFile.toFile());
            newJob.setContainerImageDescriptor(FileUtils.readFileToString(tempFile.toFile(), StandardCharsets.UTF_8));
        }
        else if(workflowDescriptor.indexOf("dockstore") != -1){ // A descriptor hosted in Dockstore
            String toolDockstoreID = workflowDescriptor;
            String dockstoreID = null;
            Client client = new Client();

            try {
                client.setupClientEnvironment(Lists.newArrayList());
            } catch (ConfigurationException e) {
                kill("consonance: need dockstore config file to schedule dockstore entries");
            }
            final File tempDir = Files.createTempDirectory("tmp").toFile();
            AbstractEntryClient actualClient = null;
            if (!toolDockstoreID.equals(null)) {
                actualClient = client.getToolClient();
                dockstoreID = toolDockstoreID;
            }else{
                kill("consonance: missing required parameter for scheduling jobs");
            }
            // This should determine whether we want to launch a cwl or wdl version of a tool
            final SourceFile cwlFromServer;
            try {
                cwlFromServer = actualClient.getDescriptorFromServer(dockstoreID, body.getWorkflowType());
                newJob.setContainerImageDescriptor(cwlFromServer.getContent());
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
        else { //String sequence contains the word 'dockstore' is a dockstore id. Otherwise is a file
            newJob.setContainerImageDescriptor(workflowDescriptor);
        }


        String workflowParams = body.getWorkflowParams(); // Runtime descriptor, <json> file.
        if (workflowParams.isEmpty()) {
            return Response.noContent().build();
        }else{
            newJob.setContainerRuntimeDescriptor(workflowParams);
        }

        String workflowType = body.getWorkflowType(); // <CWL | WDL> param.
        if (workflowType.isEmpty()) {
            return Response.noContent().build();
        }
        else {
            newJob.setContainerImageDescriptorType(workflowType);
        }


        // TODO: What optional parameters can be implemented. `Active dev...`
        // Optional parameters.

        String flavour = workflowKeyValues.get("flavour").toString();

        newJob.setFlavour(flavour);


        Ga4ghWesWorkflowRunId runId = new Ga4ghWesWorkflowRunId();


        Integer workflowsBeforeOrder = orderResource.listOwnedWorkflowRuns(user).size();

        orderResource.addOrder(user, newJob);
        Integer allWorkflows = orderResource.listOwnedWorkflowRuns(user).size();
        if(workflowsBeforeOrder < allWorkflows){
            runId.setWorkflowId(String.valueOf(orderResource.listOwnedWorkflowRuns(user).get(allWorkflows - 1).getJobId()));
            return Response.ok().entity(runId).build();
        }
        else return Response.serverError().build();


    }

    private Ga4ghWesState mapState(JobState state) {

        Ga4ghWesState ga4ghState = null;
        if (state != null) {
            switch (state) {
                case LOST:
                    ga4ghState = Ga4ghWesState.SYSTEMERROR;
                    return ga4ghState;

                case START:
                    ga4ghState = Ga4ghWesState.INITIALIZING;
                    return ga4ghState;

                case FAILED:
                    ga4ghState = Ga4ghWesState.ERROR;
                    return ga4ghState;

                case SUCCESS:
                    ga4ghState = Ga4ghWesState.COMPLETE;
                    return ga4ghState;

                case RUNNING:
                    ga4ghState = Ga4ghWesState.RUNNING;
                    return ga4ghState;

                case PENDING:
                    ga4ghState = Ga4ghWesState.QUEUED;
                    return ga4ghState;

            }
        }
        return Ga4ghWesState.UNKNOWN;
    }

}
