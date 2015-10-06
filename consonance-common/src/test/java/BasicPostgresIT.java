/*
 * Copyright (C) 2015 CancerCollaboratory
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import io.consonance.common.BasicPostgreSQL;
import io.consonance.common.CommonTestUtilities;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dyuen
 */
public class BasicPostgresIT {
    private BasicPostgreSQL postgres;

    @BeforeClass
    public static void setup() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = CommonTestUtilities.parseConfig(configFile.getAbsolutePath());
        this.postgres = new BasicPostgreSQL(parseConfig);

        // clean up the database
        postgres.clearDatabase();
    }

    @After
    public void tearDown() throws Exception {
    }


    /**
     * Test of clearDatabase method, of class PostgreSQL.
     */
    @Test
    public void testClearDatabase() {
        postgres.clearDatabase();
    }
}
