package io.swagger.api;

import io.swagger.annotations.ApiParam;
import io.swagger.api.factories.JobsApiServiceFactory;
import io.swagger.model.JobStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/jobs")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the jobs API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T19:50:58.742Z")
public class JobsApi  {
   private final JobsApiService delegate = JobsApiServiceFactory.getJobsApi();

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Get status for a workflow ", response = JobStatus.class, tags={ "GA4GH-workflow-execution" })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description of job including its status", response = JobStatus.class) })
    public Response jobsGet(
        @ApiParam(value = "URL to descriptor for workflow",required=true) @QueryParam("descriptor_url") String descriptorUrl,
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.jobsGet(descriptorUrl,securityContext);
    }
}
