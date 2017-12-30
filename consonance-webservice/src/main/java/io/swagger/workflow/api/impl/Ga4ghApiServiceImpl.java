package io.swagger.workflow.api.impl;

import com.google.common.base.Joiner;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.resources.OrderResource;
import io.swagger.workflow.api.*;
import io.swagger.workflow.model.*;

import io.swagger.workflow.model.Ga4ghWesServiceInfo;
import io.swagger.workflow.model.Ga4ghWesWorkflowListResponse;
import io.swagger.workflow.model.Ga4ghWesWorkflowLog;
import io.swagger.workflow.model.Ga4ghWesWorkflowRequest;
import io.swagger.workflow.model.Ga4ghWesWorkflowRunId;
import io.swagger.workflow.model.Ga4ghWesWorkflowStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import io.swagger.workflow.api.NotFoundException;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-12-24T02:51:26.646Z")
public class Ga4ghApiServiceImpl extends Ga4ghApiService {

    private static ConsonanceWebserviceConfiguration config;
    private static OrderResource orderResource;

    public static void setConfig(ConsonanceWebserviceConfiguration config) {
        Ga4ghApiServiceImpl.config = config;
    }

    public static void setOrderResource(OrderResource orderResource) {
        Ga4ghApiServiceImpl.orderResource = orderResource;
    }

    @Override
    public Response cancelJob(String workflowId, ConsonanceUser user) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "delete is not yet supported in Consonance!")).build();
    }
    @Override
    public Response getServiceInfo(ConsonanceUser user) throws NotFoundException {
        try {
            Ga4ghWesServiceInfo serviceInfo = new Ga4ghWesServiceInfo();

            // types
            Ga4ghWesWorkflowTypeVersion wtv = new Ga4ghWesWorkflowTypeVersion();
            wtv.addWorkflowTypeVersionItem("1.0.0");
            Map<String, Ga4ghWesWorkflowTypeVersion> map = new HashMap<String, Ga4ghWesWorkflowTypeVersion>();
            map.put("CWL", wtv);
            map.put("WDL", wtv);
            serviceInfo.setWorkflowTypeVersions(map);

            // wes version
            serviceInfo.setSupportedWesVersions(Arrays.asList("1.0.0"));

            // engine
            Map<String, String> engineMap = new HashMap<String, String>();
            // FIXME: these need to be coordinated with the versions installed by the playbook for the worker
            engineMap.put("Cromwell", "29");
            engineMap.put("cwltool", "1.0.20170828135420");
            serviceInfo.setEngineVersions(engineMap);

            // FIXME: need to confirm this list with the Dockstore CLI
            serviceInfo.setSupportedFilesystemProtocols(Arrays.asList("http", "https", "s3", "file"));

            return Response.ok().entity(serviceInfo).build();
        } catch (Exception e) {
            // FIXME: is there a better error to return here?
            System.err.println(e.toString());
            e.printStackTrace();
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
        }
    }
    @Override
    public Response getWorkflowLog(String workflowId, ConsonanceUser user) throws NotFoundException {
        try {
            Job job = orderResource.getWorkflowRun(user, workflowId);

            // log object
            Ga4ghWesWorkflowLog workflowLog = new Ga4ghWesWorkflowLog();
            workflowLog.setWorkflowId(job.getUuid());

            // workflow status
            Ga4ghWesWorkflowDesc workflow = new Ga4ghWesWorkflowDesc();
            convertStatus(job, workflow);
            workflowLog.setState(workflow.getState());

            //request
            Ga4ghWesWorkflowRequest request = new Ga4ghWesWorkflowRequest();
            request.setWorkflowDescriptor(job.getContainerImageDescriptor());
            request.setWorkflowParams(job.getContainerRuntimeDescriptor());
            request.setWorkflowType(job.getContainerImageDescriptorType());
            // FIXME: we need to track the version of WDL/CWL, just passing in empty string for now
            request.setWorkflowTypeVersion("");
            workflowLog.setRequest(request);

            // FIXME: it's going to take a lot of work to parse all this info from the various workflow engines!
            // task log will be null for now
            workflowLog.setTaskLogs(null);

            // workflow log
            Ga4ghWesLog log = new Ga4ghWesLog();
            log.setCmd(Arrays.asList("dockstore"));
            log.setStartTime(job.getCreateTimestamp().toString());
            log.setEndTime(job.getUpdateTimestamp().toString());
            // FIXME: is this properly converting to an int, 0 == success?
            log.setExitCode(job.getState().ordinal());
            log.setName("dockstore");
            log.setStderr(job.getStderr());
            log.setStdout(job.getStdout());
            workflowLog.setWorkflowLog(log);

            // outputs
            // FIXME: need to parse the outputs, pass along to Job object, and convert here
            // FIXME: is this the best way or should I pass null for now?
            workflowLog.setOutputs(Arrays.asList(new Ga4ghWesParameter()));

            return Response.ok().entity(workflowLog).build();

        } catch (Exception e) {
            // FIXME: is there a better error to return here?
            System.err.println(e.toString());
            e.printStackTrace();
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
        }
    }
    @Override
    public Response getWorkflowStatus(String workflowId, ConsonanceUser user) throws NotFoundException {
        try {
            Job job = orderResource.getWorkflowRun(user, workflowId);
            Ga4ghWesWorkflowStatus status = new Ga4ghWesWorkflowStatus();
            Ga4ghWesWorkflowDesc workflow = new Ga4ghWesWorkflowDesc();
            convertStatus(job, workflow);
            status.setState(workflow.getState());
            status.setWorkflowId(job.getUuid());
            return Response.ok().entity(status).build();
        } catch (Exception e) {
            // FIXME: is there a better error to return here?
            System.err.println(e.toString());
            e.printStackTrace();
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
        }
    }
    @Override
    public Response listWorkflows( Long pageSize,  String pageToken,  String keyValueSearch, ConsonanceUser user) throws NotFoundException {
        try {
            List<Job> jobs = null;
            Long count = null;
            if (user.isAdmin()) {
                jobs = orderResource.listWorkflowRunsPaged(pageSize, pageToken, keyValueSearch, user);
                count = orderResource.countAll();
            } else {
                jobs = orderResource.listOwnedWorkflowRunsPaged(pageSize, pageToken, keyValueSearch, user);
                count = orderResource.countOwned(user);
            }
            Ga4ghWesWorkflowListResponse workflowListResponse = new Ga4ghWesWorkflowListResponse();
            Long pageTokenLong = Long.parseLong(pageToken);
            if ((pageTokenLong + 1) * pageSize < count) {
                Long newPageToken = pageTokenLong + 1;
                workflowListResponse.setNextPageToken(newPageToken.toString());
            } else {
                // TODO: should this be null instead?
                workflowListResponse.setNextPageToken("");
            }
            for (Job job : jobs) {
                Ga4ghWesWorkflowDesc workflow = new Ga4ghWesWorkflowDesc();
                convertStatus(job, workflow);
                workflow.setWorkflowId(job.getUuid());
                workflowListResponse.addWorkflowsItem(workflow);
            }
            return Response.ok().entity(workflowListResponse).build();
        } catch (Exception e) {
            // FIXME: is there a better error to return here?
            System.err.println(e.toString());
            e.printStackTrace();
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * Converts status field from job to workflow objects
     * @param job
     * @param workflow
     */
    protected void convertStatus (Job job, Ga4ghWesWorkflowDesc workflow) {
        if (job.getState() == JobState.FAILED) {
            workflow.setState(Ga4ghWesState.ERROR);
        } else if (job.getState() == JobState.PENDING) {
            workflow.setState(Ga4ghWesState.QUEUED);
        } else if (job.getState() == JobState.LOST) {
            workflow.setState(Ga4ghWesState.SYSTEMERROR);
        } else if (job.getState() == JobState.RUNNING) {
            workflow.setState(Ga4ghWesState.RUNNING);
        } else if (job.getState() == JobState.START) {
            workflow.setState(Ga4ghWesState.INITIALIZING);
        } else if (job.getState() == JobState.SUCCESS) {
            workflow.setState(Ga4ghWesState.COMPLETE);
        } else {
            workflow.setState(Ga4ghWesState.UNKNOWN);
        }
    }

    @Override
    public Response runWorkflow(Ga4ghWesWorkflowRequest body, ConsonanceUser user) throws NotFoundException {
        // new
        try {
            Job job = new Job();
            job.setContainerImageDescriptor(body.getWorkflowDescriptor());
            job.setContainerRuntimeDescriptor(body.getWorkflowParams());
            final Job job1 = orderResource.addOrder(user, job);
            final Ga4ghWesWorkflowRunId result = new Ga4ghWesWorkflowRunId();
            result.setWorkflowId(job1.getUuid());
            //final URI uri = new URI(uriInfo.getBaseUri() + "jobs/" + job1.getUuid());
            return Response.ok().entity(result).build(); //seeOther(uri).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
        }
        // end new

        //return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
