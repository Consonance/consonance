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

import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.consonance.webservice.resources.OrderResource;
import io.swagger.workflow.api.ApiResponseMessage;
import io.swagger.workflow.api.NotFoundException;
import io.swagger.workflow.api.RunApiService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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
    public Response runPost(String descriptorUrl, SecurityContext securityContext)
    throws NotFoundException {
        // do some magic!
        //return Response.seeOther()
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
