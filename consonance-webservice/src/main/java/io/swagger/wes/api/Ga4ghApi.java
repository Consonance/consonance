package io.swagger.wes.api;

import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiParam;
import io.swagger.wes.api.factories.Ga4ghApiServiceFactory;
import io.swagger.wes.model.*;

import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


@Path("/ga4gh")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the ga4gh WES API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-09-15T17:06:31.319-07:00")
public class Ga4ghApi  {
   private final Ga4ghApiService delegate; // = Ga4ghApiServiceFactory.getGa4ghApi();

   public Ga4ghApi(){
       delegate = Ga4ghApiServiceFactory.getGa4ghApi();
   }

   public Ga4ghApi(@Context ServletConfig servletContext){
     Ga4ghApiService delegate = null;

     if (servletContext != null){
       String implClass = servletContext.getInitParameter("Ga4ghApi.implementation");
       if (implClass != null && !"".equals(implClass.trim())){
         try {
          delegate = (Ga4ghApiService) Class.forName(implClass).newInstance();
         } catch (Exception e){
          throw new RuntimeException(e);
         }
       }
     }
     if (delegate == null){
       delegate = Ga4ghApiServiceFactory.getGa4ghApi();
     }
     this.delegate = delegate;
   }
    @UnitOfWork
    @DELETE
    @Path("/wes/v1/workflows/{workflow_id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Cancel a running workflow", notes = "", response = Ga4ghWesWorkflowRunId.class, tags={ "WorkflowExecutionService", })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Ga4ghWesWorkflowRunId.class) })
    public Response cancelJob(@ApiParam(value = "",required=true) @PathParam("workflow_id") String workflowId, @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.cancelJob(workflowId, user);
    }


    @GET
    @UnitOfWork
    @Path("/wes/v1/service-info")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get information about Workflow Execution Service.  May include information related (but not limited to) the workflow descriptor formats, versions supported, the WES API versions supported, and information about general the service availability.", notes = "", response = Ga4ghWesServiceInfo.class, tags={ "WorkflowExecutionService", })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Ga4ghWesServiceInfo.class) })
    public Response getServiceInfo(@Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.getServiceInfo(user);
    }

    @UnitOfWork
    @GET
    @Path("/wes/v1/workflows/{workflow_id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get detailed info about a running workflow", notes = "", response = Ga4ghWesWorkflowLog.class, tags={ "WorkflowExecutionService", })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Ga4ghWesWorkflowLog.class) })
    public Response getWorkflowLog(@ApiParam(value = "",required=true) @PathParam("workflow_id") String workflowId, @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.getWorkflowLog(workflowId, user);
    }
    @UnitOfWork
    @GET
    @Path("/wes/v1/workflows/{workflow_id}/status")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get quick status info about a running workflow", notes = "", response = Ga4ghWesWorkflowStatus.class, tags={ "WorkflowExecutionService", })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Ga4ghWesWorkflowStatus.class) })
    public Response getWorkflowStatus(@ApiParam(value = "",required=true) @PathParam("workflow_id") String workflowId, @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.getWorkflowStatus(workflowId, user);
    }
    @UnitOfWork
    @GET
    @Path("/wes/v1/workflows")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "List the workflows, this endpoint will list the workflows in order of oldest to newest.  There is no guarantee of live updates as the user traverses the pages, the behavior should be decided (and documented) by each implementation.", notes = "", response = Ga4ghWesWorkflowListResponse.class, tags={ "WorkflowExecutionService", })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Ga4ghWesWorkflowListResponse.class) })
    public Response listWorkflows(@ApiParam(value = "OPTIONAL Number of workflows to return at once. Defaults to 256, and max is 2048.") @QueryParam("page_size") Long pageSize
,@ApiParam(value = "OPTIONAL Token to use to indicate where to start getting results. If unspecified, returns the first page of results.") @QueryParam("page_token") String pageToken
,@ApiParam(value = "OPTIONAL For each key, if the key's value is empty string then match workflows that are tagged with this key regardless of value.") @QueryParam("key_value_search") String keyValueSearch
,@Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.listWorkflows(pageSize, pageToken, keyValueSearch, user);
    }

    @UnitOfWork
    @POST
    @Path("/wes/v1/workflows")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Run a workflow, this endpoint will allow you to create a new workflow request and retrieve its tracking ID to monitor its progress.  An important assumption in this endpoint is that the workflow_params JSON will include parameterizations along with input and output files.  The latter two may be on S3, Google object storage, local filesystems, etc.  This specification makes no distinction.  However, it is assumed that the submitter is using URLs that this system both understands and can access. For Amazon S3, this could be accomplished by given the credentials associated with a WES service access to a particular bucket.  The details are important for a production system and user on-boarding but outside the scope of this spec.", notes = "", response = Ga4ghWesWorkflowRunId.class, tags={ "WorkflowExecutionService", })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Ga4ghWesWorkflowRunId.class) })
    public Response runWorkflow(@ApiParam(value = "", required=true) Ga4ghWesWorkflowRequest body, @Auth ConsonanceUser user)
            throws NotFoundException, IOException, TimeoutException {
        return delegate.runWorkflow(body, user);
    }
}
