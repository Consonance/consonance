package io.consonance.common;

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
