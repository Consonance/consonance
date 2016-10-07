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

import com.google.common.base.Joiner;
import io.consonance.arch.beans.Job;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.resources.OrderResource;
import io.swagger.workflow.api.NotFoundException;
import io.swagger.workflow.api.RunApiService;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-06-29T18:39:51.024Z")
public class RunApiServiceImpl extends RunApiService {
    private static ConsonanceWebserviceConfiguration config;
    private static OrderResource orderResource;

    public static void setConfig(ConsonanceWebserviceConfiguration config) {
        RunApiServiceImpl.config = config;
    }

    public static void setOrderResource(OrderResource orderResource) {
        RunApiServiceImpl.orderResource = orderResource;
    }

    @Override
    public Response runPost(String wf, InputStream inputStream, FormDataContentDisposition fileDetail,
            ConsonanceUser user, UriInfo uriInfo) throws NotFoundException {
        try {
            final List<String> strings = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
            final String join = Joiner.on('\n').join(strings);
            Job job = new Job();
            job.setContainerImageDescriptor(wf);
            job.setContainerRuntimeDescriptor(join);
            final Job job1 = orderResource.addOrder(user, job);
            final URI uri = new URI(uriInfo.getBaseUri() + "jobs/" + job1.getUuid());
            return Response.seeOther(uri).build();
        } catch (IOException | URISyntaxException e) {
            throw new WebApplicationException(e, HttpStatus.SC_BAD_REQUEST);
        }
    }
}
