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
import io.consonance.common.Constants;
import io.consonance.common.Utilities;
import io.consonance.webservice.ConsonanceWebserviceApplication;
import io.consonance.webservice.ConsonanceWebserviceConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Test the main method in conjunction with a real workflow.
 *
 * @author dyuen
 */
public class WebserviceAuthorizedIT {

    @Before
    public void clearState() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
    }

    @ClassRule
    public static final DropwizardAppRule<ConsonanceWebserviceConfiguration> RULE =
            new DropwizardAppRule<>(ConsonanceWebserviceApplication.class, ResourceHelpers.resourceFilePath("run-fox.yml"));

    @Test
    public void testMetadataAuthorized() throws Exception {
        // get authorization token
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        final String token = parseConfig.getString(Constants.WEBSERVICE_TOKEN);

//        // do actual check
//        Client client = null;
//        try {
//            client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client2");
//            Response response = client.target(String.format("http://localhost:%d/configuration", RULE.getLocalPort())).request()
//                    .header("Authorization", "Bearer " + token).get();
//            assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
//        } finally {
//            client.close();
//        }
    }

}
