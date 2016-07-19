package io.swagger.task.api;

import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiParam;
import io.swagger.task.api.factories.V1ApiServiceFactory;
import io.swagger.task.model.Ga4ghTaskExecJob;
import io.swagger.task.model.Ga4ghTaskExecJobId;
import io.swagger.task.model.Ga4ghTaskExecJobListResponse;
import io.swagger.task.model.Ga4ghTaskExecTask;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/v1")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the v1 API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public class V1Api  {
   private final V1ApiService delegate = V1ApiServiceFactory.getV1Api();

    @UnitOfWork
    @DELETE
    @Path("/jobs/{value}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Cancel a running task", notes = "", response = Ga4ghTaskExecJobId.class, tags={ "TaskService",  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJobId.class) })
    public Response cancelJob(
        @ApiParam(value = "",required=true) @PathParam("value") String value,
        @Auth ConsonanceUser user)
    throws NotFoundException {
     return delegate.cancelJob(value,user);
    }

    @UnitOfWork
    @GET
    @Path("/jobs/{value}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get info about a running task", notes = "", response = Ga4ghTaskExecJob.class, tags={ "TaskService",  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJob.class) })
    public Response getJob(
        @ApiParam(value = "",required=true) @PathParam("value") String value, @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.getJob(value,user);
    }

    @UnitOfWork
    @GET
    @Path("/jobs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "List the TaskOps", notes = "", response = Ga4ghTaskExecJobListResponse.class, tags={ "TaskService",  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJobListResponse.class) })
    public Response listJobs(@Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.listJobs(user);
    }

    @UnitOfWork
    @POST
    @Path("/jobs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Run a task", notes = "", response = Ga4ghTaskExecJobId.class, tags={ "TaskService" })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description", response = Ga4ghTaskExecJobId.class) })
    public Response runTask(
        @ApiParam(value = "" ,required=true) Ga4ghTaskExecTask body,
            @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.runTask(body,user);
    }
}
