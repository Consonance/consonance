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

package io.consonance.client;

import io.consonance.common.Constants;
import io.swagger.client.ApiClient;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * This will eventually be our web client for the consonance web service.
 */
public class WebClient extends ApiClient {
    public WebClient() {
        super();
    }

    public WebClient(HierarchicalINIConfiguration parseConfig) throws IOException, TimeoutException{
        this(parseConfig.getString(Constants.WEBSERVICE_BASE_PATH), parseConfig.getString(Constants.WEBSERVICE_TOKEN));
    }

    public WebClient(String basePath, String token){
        setBasePath(basePath);
        addDefaultHeader("Authorization", "Bearer " + token);
    }
}
