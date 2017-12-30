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
import java.util.List;
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
    public Response cancelJob(String workflowId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getServiceInfo(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getWorkflowLog(String workflowId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getWorkflowStatus(String workflowId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
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
                workflow.setWorkflowId(job.getUuid());
                workflowListResponse.addWorkflowsItem(workflow);
            }
            return Response.ok().entity(workflowListResponse).build();
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
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
