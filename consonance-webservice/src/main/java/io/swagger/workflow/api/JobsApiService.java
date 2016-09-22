package io.swagger.workflow.api;

import io.consonance.webservice.core.ConsonanceUser;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-11T20:20:51.265Z")
public abstract class JobsApiService {
      public abstract Response jobsDescriptorUrlGet(String descriptorUrl, ConsonanceUser securityContext, UriInfo uriInfo)
      throws NotFoundException;
}
