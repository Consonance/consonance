package io.swagger.workflow.api;

import io.consonance.webservice.core.ConsonanceUser;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-11T20:20:51.265Z")
public abstract class RunApiService {
      public abstract Response runPost(String wf, InputStream inputStream, FormDataContentDisposition fileDetail,
              ConsonanceUser securityContext, UriInfo uriInfo) throws NotFoundException;
}
