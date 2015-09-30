package io.consonance.client;

import io.consonance.common.Constants;
import io.consonance.common.Utilities;
import io.swagger.client.ApiClient;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.mortbay.thread.Timeout;

import java.io.File;
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
