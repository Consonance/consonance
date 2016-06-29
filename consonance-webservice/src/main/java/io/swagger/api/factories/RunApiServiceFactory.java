package io.swagger.api.factories;

import io.swagger.api.RunApiService;
import io.swagger.api.impl.RunApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T18:39:51.024Z")
public class RunApiServiceFactory {

   private final static RunApiService service = new RunApiServiceImpl();

   public static RunApiService getRunApi()
   {
      return service;
   }
}
