package io.consonance.client.cli;

import io.consonance.client.WebClientTest;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

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
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stream));

        Main main = new Main();
        main.setWebClient(WebClientTest.getTestingWebClient(dropwizard));
        main.runMain(new String[] { "--metadata" });

        // reset system.out
        System.setOut(System.out);
        // check out the output
        assertTrue(stream.toString().contains("foobar"));
    }

    @Test
    public void testQuietGetConfiguration() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stream));

        Main main = new Main();
        main.setWebClient(WebClientTest.getTestingWebClient(dropwizard));
        main.runMain(new String[] { "--debug", "--metadata" });

        // reset system.out
        System.setOut(System.out);
        // check out the output
        final String s = stream.toString();
        assertTrue(s.contains("foobar"));
        assertTrue(s.contains("Client response received on thread"));
        assertTrue(s.contains("Sending client request on thread"));
    }

    @Test
    public void testDebugGetConfiguration() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stream));

        Main main = new Main();
        main.setWebClient(WebClientTest.getTestingWebClient(dropwizard));
        main.runMain(new String[] { "--quiet","--metadata" });

        // config output doesn't change for quiet, it just shouldn't crash
        // reset system.out
        System.setOut(System.out);
        // check out the output
        assertTrue(stream.toString().contains("foobar"));
    }

}
