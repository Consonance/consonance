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
package io.consonance.webservice.resources;

import com.codahale.metrics.annotation.Timed;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.beans.Order;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.jdbi.JobDAO;
import io.consonance.webservice.jdbi.ProvisionDAO;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The order resource handles operations with jobs. Jobs are scheduled and can be queried to get information on the current state of the
 * job.
 *
 * @author dyuen
 */
@Path("/order")
@Api(value = "/order", tags = "order")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
    public static final int DEFAULT_DISKSPACE = 1024;
    public static final int DEFAULT_MEMORY = 128;
    public static final int DEFAULT_NUM_CORES = 8;
    private final JobDAO dao;
    private final HierarchicalINIConfiguration settings;
    private final String queueName;
    private final ProvisionDAO provisionDAO;
    private Channel jchannel = null;

    private static final Logger LOG = LoggerFactory.getLogger(OrderResource.class);

    public OrderResource(JobDAO dao, ProvisionDAO provisionDAO, String consonanceConfigFile) {
        this.dao = dao;
        this.provisionDAO = provisionDAO;
        this.settings = CommonTestUtilities.parseConfig(consonanceConfigFile);
        this.queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
    }

    @GET
    @Path("/listOwned")
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List all jobs owned by the logged-in consonanceUser", notes = "List the jobs owned by the consonanceUser", response = Job.class, responseContainer = "List", authorizations = @Authorization(value = "api_key"))
    public List<Job> listOwnedWorkflowRuns(@ApiParam(hidden=true) @Auth ConsonanceUser consonanceUser) {
        return dao.findAll(consonanceUser.getName());
    }

    @GET
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List all known jobs", notes = "List all jobs", response = Job.class, responseContainer = "List", authorizations = @Authorization(value = "api_key"))
    public List<Job> listWorkflowRuns(@ApiParam(hidden=true) @Auth ConsonanceUser consonanceUser) {
        if (consonanceUser.isAdmin()) {
            return dao.findAll();
        }
        throw new WebApplicationException(HttpStatus.SC_FORBIDDEN);
    }

    @GET
    @Path("/{jobUUID}")
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List a specific job", notes = "List a specific job", response = Job.class, authorizations = @Authorization(value = "api_key"))
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = "Invalid ID supplied"),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = "Job not found") })
    public Job getWorkflowRun(@ApiParam(hidden=true) @Auth ConsonanceUser consonanceUser, @ApiParam(value = "UUID of job that needs to be fetched", required = true) @PathParam("jobUUID") String uuid) {
        final Job jobByUUID = dao.findJobByUUID(uuid);
        if (consonanceUser.isAdmin() || consonanceUser.getName().equals(jobByUUID.getEndUser())){
            return jobByUUID;
        }
        throw new WebApplicationException(HttpStatus.SC_NOT_FOUND);
    }

    @POST
    @Timed
    @UnitOfWork
    @ApiOperation(value = "Schedule a new order")
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_METHOD_NOT_ALLOWED, message = "Invalid input") })
    public Job addOrder(@ApiParam(hidden = true) @Auth ConsonanceUser consonanceUser,
            @ApiParam(value = "Order that needs to be added to the store", required = true) Job job) {
        // enforce that users schedule jobs as themselves
        job.setEndUser(consonanceUser.getName());

        int cores = DEFAULT_NUM_CORES;
        int memGb = DEFAULT_MEMORY;
        int storageGb = DEFAULT_DISKSPACE;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");

        Order newOrder = new Order();
        job.setState(JobState.START);
        job.setUuid(UUID.randomUUID().toString().toLowerCase());
        newOrder.setJob(job);
        Provision provision = new Provision(cores, memGb, storageGb, a);
        newOrder.setProvision(provision);
        newOrder.getProvision().setJobUUID(newOrder.getJob().getUuid());

        if (jchannel == null || !jchannel.isOpen()) {
            try {
                this.jchannel = CommonServerTestUtilities.setupQueue(settings, queueName + "_orders");
            } catch (InterruptedException ex) {
                throw new InternalServerErrorException();
            }
        }

        final int jobId = dao.create(job);
        Job createdJob = dao.findById(jobId);
        provisionDAO.create(provision);
        LOG.debug("created job with " + createdJob.getExtraFiles().size() + " entries");

        try {
            LOG.info("\nSENDING JOB:\n '" + job + "'\n" + this.jchannel + " \n");
            this.jchannel.basicPublish("", queueName + "_orders", MessageProperties.PERSISTENT_TEXT_PLAIN,
                    newOrder.toJSON().getBytes(StandardCharsets.UTF_8));
            jchannel.waitForConfirms();

            // censor the extra files with isKeep set to false after the message is sent
            // censor extra files tagged with keep=false
            job.setExtraFiles(job.getExtraFiles().entrySet().stream().filter(e -> e.getValue().isKeep())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            LOG.debug("job in DB should be updated to " + job.getExtraFiles().size() + " entries");

        } catch (IOException | InterruptedException ex) {
            LOG.error(ex.toString());
        }
        return createdJob;
    }

}
