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

import io.consonance.common.CommonTestUtilities;
import io.consonance.webservice.ConsonanceWebserviceApplication;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

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
//        try {
//            client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");
//            Response response = client.target(String.format("http://localhost:%d/configuration", RULE.getLocalPort())).request().get();
//            assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
//        } finally {
//            client.close();
//        }
    }

}
