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
import com.rabbitmq.client.Channel;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.Constants;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This resource simply reports on the configuration and environment for the web service.
 * @author dyuen
 */
@Path("/configuration")
@Api(value = "/configuration", tags = "configuration")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource {
    private final HierarchicalINIConfiguration settings;
    private final String queueName;
    private final ConsonanceWebserviceConfiguration config;
    private Channel jchannel = null;

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResource.class);

    public ConfigurationResource(ConsonanceWebserviceConfiguration config) {
        this.config = config;
        this.settings = CommonServerTestUtilities.parseConfig(config.getConsonanceConfig());
        this.queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
    }

    @GET
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List configuration", notes = "List the jobs owned by the consonanceUser", response = String.class, responseContainer = "SortedMap", authorizations = @Authorization(value = "api_key"))
    public SortedMap<String, String> listConfiguration(@ApiParam(hidden = true) @Auth ConsonanceUser consonanceUser) {
        if (consonanceUser.isAdmin()) {
            SortedMap<String, String> environment = new TreeMap<>();
            // handle dropwizard config
            environment.put("consonance.authenticationCachePolicy", config.getAuthenticationCachePolicy().toParsableString());
            environment.put("consonance.database.url", config.getDataSourceFactory().getUrl());
            environment.put("consonance.database.user", config.getDataSourceFactory().getUrl());
            environment.put("consonance.database.password", config.getDataSourceFactory().getUrl());
            environment.put("consonance.database.password", config.getDataSourceFactory().getUrl());
            
            environment.put("consonance.httpclient", config.getHttpClientConfiguration().toString());
            environment.put("consonance.consonanceConfig", config.getConsonanceConfig());

            // handle consonance config
            final Iterator<String> keys = settings.getKeys();
            keys.forEachRemaining(key -> environment.put(key, settings.getString(key)));
            return environment;
        }
        throw new WebApplicationException(HttpStatus.SC_FORBIDDEN);
    }
}
