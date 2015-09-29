import io.consonance.client.WebClient;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.dropwizard.testing.junit.DropwizardClientRule;
import io.swagger.client.ApiException;
import io.swagger.client.api.UserApi;
import io.swagger.client.model.ConsonanceUser;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These tests mock up a Dropwizard instance in order to unit test the client.
 * @author dyuen
 */
public class WebClientTest {

    @Path("/user")
    public static class PingResource {
        @GET
        public String mockUsers() {
            return "[{\n" + "    \"create_timestamp\": \"2015-09-25T21:54:33.504+0000\",\n"
                    + "    \"update_timestamp\": \"2015-09-25T21:54:33.504+0000\",\n" + "    \"user_id\": 2,\n"
                    + "    \"name\": \"funkytown@funky.com\",\n" + "    \"admin\": false,\n"
                    + "    \"hashed_password\": \"1affeec8484f98373804135ddc136921f7358e85048529d537406e1819b7aae9\"\n" + "  }]";
        }
    }

    @ClassRule
    public final static DropwizardClientRule dropwizard = new DropwizardClientRule(new PingResource());

    private WebClient getWebClient() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = CommonTestUtilities.parseConfig(configFile.getAbsolutePath());
        WebClient client = new WebClient();
        String root = dropwizard.baseUri().toURL().toString();
        client.setBasePath(root);
        client.addDefaultHeader("Authorization", "Bearer " + parseConfig.getString(Constants.WEBSERVICE_TOKEN));
        return client;
    }

    @Test
    public void testListUsers() throws ApiException, IOException, TimeoutException {
        WebClient client = getWebClient();
        UserApi userApi = new UserApi(client);
        final List<ConsonanceUser> consonanceUsers = userApi.listUsers();
        // should just be the one admin user after we clear it out
        assertThat(consonanceUsers.size() > 1);
    }
}