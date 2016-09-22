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
        assertTrue(s, s.contains("foobar"));
        assertTrue(s, s.contains("Sending client request on thread"));
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
