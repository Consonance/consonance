package io.swagger.workflow.api.factories;

import io.swagger.workflow.api.Ga4ghApiService;
import io.swagger.workflow.api.impl.Ga4ghApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-12-24T02:51:26.646Z")
public class Ga4ghApiServiceFactory {
    private final static Ga4ghApiService service = new Ga4ghApiServiceImpl();

    public static Ga4ghApiService getGa4ghApi() {
        return service;
    }
}
