package io.consonance.client;

import com.google.common.base.Joiner;
import io.consonance.common.ITUtilities;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.JobApi;
import io.swagger.client.model.Job;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

/**
 * This will eventually be our web client for the consonance web service.
 */
public class WebClient extends ApiClient {
    public WebClient() {
        super();
    }

    public static void main(String args[]) throws ApiException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = ITUtilities.parseConfig(configFile.getAbsolutePath());

        WebClient client = new WebClient();
        client.setBasePath("http://localhost:8080");
        client.addDefaultHeader("Authorization", "Bearer 8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
        JobApi jobApi = new JobApi(client);
        final List<Job> jobs = jobApi.listOwnedWorkflowRuns();
        Joiner joiner = Joiner.on("; ").skipNulls();
        System.out.println(joiner.join(jobs));

    }
}
