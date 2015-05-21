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
package info.pancancer.arch3.worker;

import info.pancancer.arch3.coordinator.Coordinator;
import info.pancancer.arch3.jobGenerator.JobGeneratorDEWorkflow;
import info.pancancer.arch3.utils.ITUtilities;
import java.io.File;
import java.io.IOException;
import joptsimple.OptionException;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class WorkerIT {

    @BeforeClass
    public static void setup() throws IOException {
        ITUtilities.clearState();
    }

    /**
     * Test of main method, of class Worker.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = OptionException.class)
    public void testHelpMessage() throws Exception {
        Worker.main(new String[] { "--help" });
    }

    /**
     * Test of main method, of class JobGeneratorDEWorkflow.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testTestModeOperation() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config.json");
        File iniDir = FileUtils.getFile("ini");
        // prime the coordinator with an order
        JobGeneratorDEWorkflow.main(new String[] { "--config", file.getAbsolutePath(), "--ini", iniDir.getAbsolutePath() });
        // prime the worker with a job
        Coordinator.main(new String[] { "--config", file.getAbsolutePath() });
        Worker.main(new String[] { "--config", file.getAbsolutePath(), "--uuid", "12345", "--test" });
    }

}
