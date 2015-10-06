package io.consonance.client.cli;

import io.consonance.client.WebClientTest;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;


/**
 * These tests mock up a DropWizard instance in order to unit test the client.
 * @author dyuen
 */
public class MainTest {

    @Path("/configuration")
    public static class ConfigResource {
        @GET
        public String mockUsers() {
            return "{  \"rabbit.rabbitMQHost\": \"localhost\",\n" + "  \"rabbit.rabbitMQPass\": \"<redacted>\",\n"
                    + "  \"rabbit.rabbitMQQueueName\": \"consonance_arch\",\n" + "  \"rabbit.rabbitMQUser\": \"queue_user\",\n"
                    + "  \"report.namespace\": \"flying_snow\",\n"
                    + "  \"report.slack_token\": \"foobar\"\n" + "}";
        }
    }

    @ClassRule
    public final static DropwizardClientRule dropwizard = new DropwizardClientRule(new ConfigResource());


    @Test
    public void testGetConfiguration() throws Exception {
        Main main = new Main();
        main.setWebClient(WebClientTest.getTestingWebClient(dropwizard));
        main.runMain(new String[] { "--metadata" });
    }
}
