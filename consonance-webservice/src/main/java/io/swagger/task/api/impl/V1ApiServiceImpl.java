package io.swagger.task.api.impl;

import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.resources.OrderResource;
import io.swagger.task.api.ApiResponseMessage;
import io.swagger.task.api.NotFoundException;
import io.swagger.task.api.V1ApiService;
import io.swagger.task.model.Ga4ghTaskExecTask;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T20:13:58.346Z")
public class V1ApiServiceImpl extends V1ApiService {
    private static ConsonanceWebserviceConfiguration config;
    private static OrderResource orderResource;

    public static void setConfig(ConsonanceWebserviceConfiguration config) {
        V1ApiServiceImpl.config = config;
    }

    public static void setOrderResource(OrderResource orderResource) {
        V1ApiServiceImpl.orderResource = orderResource;
    }

    @Override
    public Response cancelJob(String value, SecurityContext securityContext)
    throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getJob(String value, SecurityContext securityContext)
    throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response listJobs(SecurityContext securityContext)
    throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response runTask(Ga4ghTaskExecTask body, SecurityContext securityContext)
    throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
