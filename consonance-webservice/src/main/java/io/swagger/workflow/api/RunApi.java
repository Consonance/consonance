package io.swagger.workflow.api;

import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiParam;
import io.swagger.workflow.api.factories.RunApiServiceFactory;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

@Path("/run")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the run API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-11T20:20:51.265Z")
public class RunApi  {
   private final RunApiService delegate = RunApiServiceFactory.getRunApi();

    @Context
    private UriInfo uriInfo;

    @POST
    @UnitOfWork
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Submit launch of a workflow ", response = void.class, tags={ "GA4GH-workflow-execution" })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 303, message = "Successful response", response = void.class) })
    public Response runPost(
        @ApiParam(value = "URL to descriptor for workflow",required=true) @QueryParam("wf") String wf,
        @FormDataParam("file") InputStream inputStream,
        @FormDataParam("file") FormDataContentDisposition fileDetail,
            @Auth ConsonanceUser user)
    throws NotFoundException {
        return delegate.runPost(wf,inputStream, fileDetail,user, uriInfo);
    }
}
