package io.swagger.task.api;

import io.swagger.annotations.ApiParam;
import io.swagger.task.model.Ga4ghTaskExecJob;
import io.swagger.task.model.Ga4ghTaskExecJobId;
import io.swagger.task.model.Ga4ghTaskExecJobListResponse;
import io.swagger.task.model.Ga4ghTaskExecTask;
import io.swagger.task.api.factories.V1ApiServiceFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/v1")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the v1 API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T20:13:58.346Z")
public class V1Api  {
   private final V1ApiService delegate = V1ApiServiceFactory.getV1Api();

    @DELETE
    @Path("/jobs/{value}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Cancel a running task", notes = "", response = Ga4ghTaskExecJobId.class, tags={ "TaskService",  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJobId.class) })
    public Response cancelJob(
        @ApiParam(value = "",required=true) @PathParam("value") String value,
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.cancelJob(value,securityContext);
    }
    @GET
    @Path("/jobs/{value}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get info about a running task", notes = "", response = Ga4ghTaskExecJob.class, tags={ "TaskService",  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJob.class) })
    public Response getJob(
        @ApiParam(value = "",required=true) @PathParam("value") String value,
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getJob(value,securityContext);
    }
    @GET
    @Path("/jobs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "List the TaskOps", notes = "", response = Ga4ghTaskExecJobListResponse.class, tags={ "TaskService",  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJobListResponse.class) })
    public Response listJobs(
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.listJobs(securityContext);
    }
    @POST
    @Path("/jobs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Run a task", notes = "", response = Ga4ghTaskExecJobId.class, tags={ "TaskService" })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJobId.class) })
    public Response runTask(
        @ApiParam(value = "" ,required=true) Ga4ghTaskExecTask body,
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.runTask(body,securityContext);
    }
}