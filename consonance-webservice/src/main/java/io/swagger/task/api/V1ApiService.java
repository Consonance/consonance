package io.swagger.task.api;

import io.swagger.task.model.Ga4ghTaskExecTask;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T20:13:58.346Z")
public abstract class V1ApiService {
      public abstract Response cancelJob(String value,SecurityContext securityContext)
      throws NotFoundException;
      public abstract Response getJob(String value,SecurityContext securityContext)
      throws NotFoundException;
      public abstract Response listJobs(SecurityContext securityContext)
      throws NotFoundException;
      public abstract Response runTask(Ga4ghTaskExecTask body,SecurityContext securityContext)
      throws NotFoundException;
}
