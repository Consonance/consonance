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

package io.consonance.common;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.dbutils.handlers.KeyedHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Utility methods for testing.
 *
 * @author dyuen
 */
public class CommonTestUtilities {

    public static final String DUMMY_ADMIN_PASSWORD = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

    public static HierarchicalINIConfiguration parseConfig(String path) {
        try {
            return new HierarchicalINIConfiguration(path);
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class TestingPostgres extends BasicPostgreSQL{

        public TestingPostgres(HierarchicalINIConfiguration config){
            super(config);
        }

        @Override
        public void clearDatabase(){
            super.clearDatabase();
            this.runInsertStatement(
                    "insert into consonance_user(user_id, admin, hashed_password, name) VALUES (1,true,'" + DUMMY_ADMIN_PASSWORD
                            + "','admin@admin.com');"
            , new KeyedHandler<>("user_id"));
        }
    }

    /**
     * Clears database state and known queues for testing.
     *
     * @throws IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public static void clearState() throws IOException, TimeoutException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        TestingPostgres postgres = new TestingPostgres(parseConfig);
        postgres.clearDatabase();
    }

}
