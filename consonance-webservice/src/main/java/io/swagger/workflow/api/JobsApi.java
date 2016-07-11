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

package io.swagger.workflow.api;

import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.ApiParam;
import io.swagger.workflow.api.factories.JobsApiServiceFactory;
import io.swagger.workflow.model.JobStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Path("/jobs")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the jobs API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T19:50:58.742Z")
public class JobsApi  {
   private final JobsApiService delegate = JobsApiServiceFactory.getJobsApi();

    @Context
    private UriInfo uriInfo;

    @GET
    @UnitOfWork
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Get status for a workflow ", response = JobStatus.class, tags={ "GA4GH-workflow-execution" })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Description of job including its status", response = JobStatus.class) })
    public Response jobsGet(
        @ApiParam(value = "URL to descriptor for workflow",required=true) @QueryParam("descriptor_url") String descriptorUrl,
        @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.jobsGet(descriptorUrl,securityContext, uriInfo);
    }
}
