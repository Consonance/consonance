/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.swagger.workflow.api.impl;

import com.google.gson.Gson;
import io.consonance.arch.beans.Job;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.resources.OrderResource;
import io.swagger.workflow.api.JobsApiService;
import io.swagger.workflow.api.NotFoundException;
import io.swagger.workflow.model.JobStatus;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T18:39:51.024Z")
public class JobsApiServiceImpl extends JobsApiService {

    private static final Logger LOG = LoggerFactory.getLogger(JobsApiServiceImpl.class);

    private static ConsonanceWebserviceConfiguration config;
    private static OrderResource orderResource;
    private static Gson gson = new Gson();

    public static void setConfig(ConsonanceWebserviceConfiguration config) {
        JobsApiServiceImpl.config = config;
    }

    public static void setOrderResource(OrderResource orderResource) {
        JobsApiServiceImpl.orderResource = orderResource;
    }

    @Override
    public Response jobsDescriptorUrlGet(String descriptorUrl, ConsonanceUser user, UriInfo uriInfo)
    throws NotFoundException {
        final Job workflowRun = orderResource.getWorkflowRun(user, descriptorUrl);

        if (workflowRun == null){
            throw new WebApplicationException(HttpStatus.SC_NOT_FOUND);
        }
        JobStatus status = new JobStatus();
        final String containerRuntimeDescriptor = workflowRun.getContainerRuntimeDescriptor();
        final HashMap hashMap = gson.fromJson(containerRuntimeDescriptor, HashMap.class);
        status.setInput(hashMap);

        status.setLog(uriInfo.getBaseUri() + "order/" + descriptorUrl + "/log");

        //TODO: not defined in specification
        status.setOutput(null);
        status.setRun(workflowRun.getContainerImageDescriptor());

        JobStatus.StateEnum stateEnum;

        // convert from our status to GA4GH status
        switch (workflowRun.getState()) {
        case START:
            stateEnum = JobStatus.StateEnum.RUNNING;
            break;
        case PENDING:
            stateEnum = JobStatus.StateEnum.RUNNING;
            break;
        case RUNNING:
            stateEnum = JobStatus.StateEnum.RUNNING;
            break;
        case SUCCESS:
            stateEnum = JobStatus.StateEnum.SUCCESS;
            break;
        case FAILED:
            stateEnum = JobStatus.StateEnum.FAILED;
            break;
        case LOST:
            stateEnum = JobStatus.StateEnum.PAUSED;
            break;
        default:
            stateEnum = null;
        }

        status.setState(stateEnum);
        // convert from order to GA4GH workflow
        return Response.ok().entity(status).build();
    }
}
