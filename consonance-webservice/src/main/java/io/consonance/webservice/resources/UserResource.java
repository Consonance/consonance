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
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.jdbi.ConsonanceUserDAO;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Random;


/**
 * This resource manages users. This should be admin access only.
 *
 * @author dyuen
 */
@Path("/user")
@Api(value = "/user", tags = "user")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private final ConsonanceUserDAO dao;

    private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);

    public UserResource(ConsonanceUserDAO dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List all known users", notes = "List all users", response = ConsonanceUser.class, responseContainer = "List", authorizations = @Authorization(value = "api_key"))
    public List<ConsonanceUser> listUsers(@ApiParam(hidden=true) @Auth ConsonanceUser user) {
        if (user.isAdmin()) {
            return dao.findAll();
        }
        throw new WebApplicationException(HttpStatus.SC_FORBIDDEN);
    }

    @GET
    @Path("/{name}")
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List a specific user", notes = "List a specific user", response = ConsonanceUser.class, authorizations = @Authorization(value = "api_key"))
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = "Invalid ID supplied"),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = "name not found") })
    public ConsonanceUser getUser(@ApiParam(hidden=true) @Auth ConsonanceUser user,
            @ApiParam(value = "name of a user that needs to be fetched", required = true) @PathParam("name") String name) {
        if (user.isAdmin()) {
            return dao.findUserByName(name);
        }
        throw new WebApplicationException(HttpStatus.SC_FORBIDDEN);
    }

    @POST
    @Timed
    @UnitOfWork
    @ApiOperation(value = "Add a new user")
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_METHOD_NOT_ALLOWED, message = "Invalid input") })
    public ConsonanceUser addUser(@ApiParam(hidden=true) @Auth ConsonanceUser authUser,
            @ApiParam(value = "User that needs to be added", required = true) ConsonanceUser user) {
        if (authUser.isAdmin()) {
	    final Random random = new Random();
	    final int bufferLength = 1024;
	    final byte[] buffer = new byte[bufferLength];
	    random.nextBytes(buffer);
	    String randomString = BaseEncoding.base64Url().omitPadding().encode(buffer);
            final String hashedPassword = Hashing.sha256().hashString(user.getName() + randomString, Charsets.UTF_8).toString();
            user.setHashedPassword(hashedPassword);
            final int jobID = dao.create(user);
            return dao.findById(jobID);
        }
        throw new WebApplicationException(HttpStatus.SC_FORBIDDEN);
    }

}
