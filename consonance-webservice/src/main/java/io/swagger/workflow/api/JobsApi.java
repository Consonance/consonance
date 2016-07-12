package io.swagger.workflow.api;

import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiParam;
import io.swagger.workflow.api.factories.JobsApiServiceFactory;
import io.swagger.workflow.model.JobStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/jobs")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the jobs API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-11T20:20:51.265Z")
public class JobsApi  {
   private final JobsApiService delegate = JobsApiServiceFactory.getJobsApi();

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/{jobId}")
    @UnitOfWork
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Get status for a workflow ", response = JobStatus.class, tags={ "GA4GH-workflow-execution" })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description of job including its status", response = JobStatus.class) })
    public Response jobsDescriptorUrlGet(
        @ApiParam(value = "URL to descriptor for workflow",required=true) @PathParam("jobId") String jobId,
            @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.jobsDescriptorUrlGet(jobId, user, uriInfo);
    }
}
