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
package info.pancancer.arch3.coordinator;

import info.pancancer.arch3.jobGenerator.JobGenerator;
import info.pancancer.arch3.utils.ITUtilities;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import joptsimple.OptionException;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class CoordinatorIT {

    @BeforeClass
    public static void setup() throws IOException, TimeoutException {
        ITUtilities.clearState();
    }

    /**
     * Test of main method, of class Coordinator.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = OptionException.class)
    public void testMainUsage() throws Exception {
        Coordinator.main(new String[] { "--help" });
    }

    /**
     * Test of doWork method, of class Coordinator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testNormalUsage() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config");
        File iniDir = FileUtils.getFile("ini");
        // prime the pump with a job
        JobGenerator.main(new String[] { "--config", file.getAbsolutePath(), "--ini", iniDir.getAbsolutePath(), "--workflow-name",
                "DEWrapper", "--workflow-version", "1.0.0", "--workflow-path",
                "/workflows/Workflow_Bundle_DEWrapperWorkflow_1.0.0_SeqWare_1.1.0" });
        // note that without a worker running in parallel, the results job will time out
        Coordinator.main(new String[] { "--config", file.getAbsolutePath() });
    }

}
