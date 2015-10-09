package io.consonance.client.cli;

import io.consonance.common.CommonTestUtilities;
import io.consonance.webservice.ConsonanceWebserviceApplication;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the main method in conjunction with a real workflow.
 *
 * @author dyuen
 */
public class WebserviceUnauthorizedIT {

    @Before
    public void clearState() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
    }

    @ClassRule
    public static final DropwizardAppRule<ConsonanceWebserviceConfiguration> RULE =
            new DropwizardAppRule<>(ConsonanceWebserviceApplication.class, ResourceHelpers.resourceFilePath("run-fox.yml"));

    @Test
    public void testMetadataNotAuthorized() throws Exception {
        Client client = null;
        try {
            client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");
            Response response = client.target(String.format("http://localhost:%d/configuration", RULE.getLocalPort())).request().get();
            assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
        } finally {
            client.close();
        }
    }

}
