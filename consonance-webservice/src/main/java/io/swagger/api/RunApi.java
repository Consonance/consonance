package io.swagger.api;

import io.swagger.annotations.ApiParam;
import io.swagger.api.factories.RunApiServiceFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/run")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the run API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T18:39:51.024Z")
public class RunApi  {
   private final RunApiService delegate = RunApiServiceFactory.getRunApi();

    @POST
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Submit launch of a workflow ", response = void.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Successful response", response = void.class) })
    public Response runPost(
        @ApiParam(value = "URL to descriptor for workflow",required=true) @QueryParam("descriptor_url") String descriptorUrl,
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.runPost(descriptorUrl,securityContext);
    }
}
