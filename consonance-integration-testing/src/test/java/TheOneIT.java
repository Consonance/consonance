import io.consonance.client.WebClient;
import io.consonance.common.Constants;
import io.consonance.common.ITUtilities;
import io.consonance.webservice.ConsonanceWebserviceApplication;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.swagger.client.ApiException;
import io.swagger.client.api.UserApi;
import io.swagger.client.model.ConsonanceUser;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One integration test to test everything.
 *
 * @author dyuen
 */
public class TheOneIT {

    @ClassRule
    public static final DropwizardAppRule<ConsonanceWebserviceConfiguration> RULE =
            new DropwizardAppRule<>(ConsonanceWebserviceApplication.class, ResourceHelpers.resourceFilePath("run-fox.yml"));

    @Test
    public void testListJobs() throws ApiException, IOException, TimeoutException {
        ITUtilities.clearState();

        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = ITUtilities.parseConfig(configFile.getAbsolutePath());

        WebClient client = new WebClient();
        client.setBasePath(parseConfig.getString(Constants.WEBSERVICE_BASE_PATH));
        client.addDefaultHeader("Authorization", "Bearer " + parseConfig.getString(Constants.WEBSERVICE_TOKEN));
        UserApi userApi = new UserApi(client);
        final List<ConsonanceUser> consonanceUsers = userApi.listUsers();
        // should just be the one admin user after we clear it out
        assertThat(consonanceUsers.size() > 1);
    }
}
