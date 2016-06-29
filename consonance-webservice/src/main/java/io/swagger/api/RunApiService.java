package io.swagger.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T19:50:58.742Z")
public abstract class RunApiService {
      public abstract Response runPost(String descriptorUrl,SecurityContext securityContext)
      throws NotFoundException;
}
