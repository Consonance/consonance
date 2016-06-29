package io.swagger.api.factories;

import io.swagger.api.JobsApiService;
import io.swagger.api.impl.JobsApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T18:39:51.024Z")
public class JobsApiServiceFactory {

   private final static JobsApiService service = new JobsApiServiceImpl();

   public static JobsApiService getJobsApi()
   {
      return service;
   }
}
