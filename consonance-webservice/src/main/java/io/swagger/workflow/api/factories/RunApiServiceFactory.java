package io.swagger.workflow.api.factories;

import io.swagger.workflow.api.RunApiService;
import io.swagger.workflow.api.impl.RunApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-11T20:20:51.265Z")
public class RunApiServiceFactory {

   private final static RunApiService service = new RunApiServiceImpl();

   public static RunApiService getRunApi()
   {
      return service;
   }
}
