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
import io.swagger.task.model.Ga4ghTaskExecJobLog;
import io.swagger.task.model.Ga4ghTaskExecState;
import io.swagger.task.model.Ga4ghTaskExecTask;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

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
        final Job workflowRun = orderResource.getWorkflowRun(user, value);
        job.setJobId(workflowRun.getUuid());
        Ga4ghTaskExecJobLog log = new Ga4ghTaskExecJobLog();
        log.setStdout(workflowRun.getStdout());
        log.setStderr(workflowRun.getStderr());
        List<Ga4ghTaskExecJobLog> logs = new ArrayList<>();
        logs.add(log);
        job.setLogs(logs);
        //job.setMetadata();
        Ga4ghTaskExecState state;
        switch (workflowRun.getState()) {
        case START:
            state = Ga4ghTaskExecState.Queued;
            break;
        case PENDING:
            state = Ga4ghTaskExecState.Queued;
            break;
        case RUNNING:
            state = Ga4ghTaskExecState.Running;
            break;
        case SUCCESS:
            state = Ga4ghTaskExecState.Complete;
            break;
        case FAILED:
            state = Ga4ghTaskExecState.Error;
            break;
        case LOST:
            state = Ga4ghTaskExecState.Unknown;
            break;
        default:
            state = Ga4ghTaskExecState.Unknown;
        }
        job.setState(state);
        Ga4ghTaskExecTask task = new Ga4ghTaskExecTask();
//        task.setDescription();
//        task.setDocker();
//        task.setInputs();
//        task.setName();
//        task.setOutputs();
//        task.setProjectId();
//        task.setResources();
//        task.setTaskId();
        job.setTask(task);
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

//        final String description = body.getDescription();
//        final List<Ga4ghTaskExecDockerExecutor> docker = body.getDocker();
//        final List<Ga4ghTaskExecTaskParameter> inputs = body.getInputs();
//        final String name = body.getName();
//        final List<Ga4ghTaskExecTaskParameter> outputs = body.getOutputs();
//        final Ga4ghTaskExecResources resources = body.getResources();

        Job job = new Job();
        job.setEndUser(user.getName());
        orderResource.addOrder(user, job);


        Ga4ghTaskExecJobId id = new Ga4ghTaskExecJobId();
        return Response.ok().entity(id).build();
    }
}
