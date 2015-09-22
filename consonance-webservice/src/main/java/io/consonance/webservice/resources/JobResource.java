/*
 * Copyright (C) 2015 Consonance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.consonance.webservice.resources;

import com.codahale.metrics.annotation.Timed;
import io.consonance.arch.beans.Job;
import io.consonance.webservice.jdbi.JobDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;

/**
 * The token resource handles operations with jobs. Jobs are scheduled and can be queried to get information on the
 * current state of the job.
 *
 * @author dyuen
 */
@Path("/job")
@Api(value = "/job", tags = "job")
@Produces(MediaType.APPLICATION_JSON)
public class JobResource {
    private final JobDAO dao;

    public JobResource(JobDAO dao) {
        this.dao = dao;
    }

    @GET
    @Path("/listOwned")
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List all jobs owned by the logged-in user", notes = "List the jobs owned by the user", response = Job.class, responseContainer = "List", authorizations = @Authorization(value = "api_key"))
    public List<Job> listOwnedWorkflowRuns() {
        throw new UnsupportedOperationException();
    }

    @GET
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List all known jobs", notes = "List all jobs", response = Job.class, responseContainer = "List", authorizations = @Authorization(value = "api_key"))
    public List<Job> listWorkflowRuns() {
        return dao.findAll();
    }

    @POST
    @ApiOperation(value = "Schedule a new workflow run")
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_METHOD_NOT_ALLOWED, message = "Invalid input") })
    public Job addWorkflowRun(@ApiParam(value = "Workflow run that needs to be added to the store", required = true) Job job) {
        throw new UnsupportedOperationException();
    }

}
