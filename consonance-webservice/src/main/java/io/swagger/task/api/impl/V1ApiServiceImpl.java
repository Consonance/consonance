package io.swagger.task.api.impl;

import io.consonance.arch.beans.Job;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.resources.OrderResource;
import io.swagger.task.api.NotFoundException;
import io.swagger.task.api.V1ApiService;
import io.swagger.task.model.Ga4ghTaskExecJob;
import io.swagger.task.model.Ga4ghTaskExecJobId;
import io.swagger.task.model.Ga4ghTaskExecJobListResponse;
import io.swagger.task.model.Ga4ghTaskExecTask;

import javax.ws.rs.core.Response;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-07-12T15:19:07.784Z")
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
    public Response cancelJob(String value, ConsonanceUser user)
    throws NotFoundException {
        final Job workflowRun = orderResource.getWorkflowRun(user, value);
        // no-op, Consonance doesn't really support cancellation
        Ga4ghTaskExecJobId id = new Ga4ghTaskExecJobId();
        id.setValue(workflowRun.getUuid());
        return Response.ok().entity(id).build();
    }
    @Override
    public Response getJob(String value, ConsonanceUser user)
    throws NotFoundException {
        Ga4ghTaskExecJob job = new Ga4ghTaskExecJob();
        return Response.ok().entity(job).build();
    }
    @Override
    public Response listJobs(ConsonanceUser user)
    throws NotFoundException {
        Ga4ghTaskExecJobListResponse list = new Ga4ghTaskExecJobListResponse();
        return Response.ok().entity(list).build();
    }
    @Override
    public Response runTask(Ga4ghTaskExecTask body, ConsonanceUser user)
    throws NotFoundException {
        Ga4ghTaskExecJobId id = new Ga4ghTaskExecJobId();
        return Response.ok().entity(id).build();
    }
}
