package io.swagger.task.api;
import io.consonance.webservice.core.ConsonanceUser;
import io.swagger.task.model.Ga4ghTaskExecTask;

import javax.ws.rs.core.Response;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
public abstract class V1ApiService {
      public abstract Response cancelJob(String value,ConsonanceUser securityContext)
      throws NotFoundException;
      public abstract Response getJob(String value,ConsonanceUser securityContext)
      throws NotFoundException;
      public abstract Response listJobs(ConsonanceUser securityContext)
      throws NotFoundException;
      public abstract Response runTask(Ga4ghTaskExecTask body,ConsonanceUser securityContext)
      throws NotFoundException;
}
