package io.swagger.task.api.factories;

import io.swagger.task.api.V1ApiService;
import io.swagger.task.api.impl.V1ApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T20:13:58.346Z")
public class V1ApiServiceFactory {

   private final static V1ApiService service = new V1ApiServiceImpl();

   public static V1ApiService getV1Api()
   {
      return service;
   }
}
