package io.swagger.wes.api.factories;

import io.swagger.wes.api.Ga4ghApiService;
import io.swagger.wes.api.impl.Ga4ghApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-09-15T17:06:31.319-07:00")
public class Ga4ghApiServiceFactory {
    private final static Ga4ghApiService service = new Ga4ghApiServiceImpl();

    public static Ga4ghApiService getGa4ghApi() {
        return service;
    }
}
