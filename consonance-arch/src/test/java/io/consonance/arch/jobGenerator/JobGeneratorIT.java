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
package io.consonance.arch.jobGenerator;

import io.consonance.common.CommonTestUtilities;
import joptsimple.OptionException;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dyuen
 */
public class JobGeneratorIT {

    @BeforeClass
    public static void setup() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
    }

    /**
     * Test of main method, of class JobGenerator.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = OptionException.class)
    public void testHelpMessage() throws Exception {
        JobGenerator.main(new String[] { "--help" });
    }

    /**
     * Test of main method, of class JobGenerator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testNormalOperation() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config");
        File iniDir = FileUtils.getFile("ini");
        JobGenerator.main(new String[] { "--config", file.getAbsolutePath(), "--ini", iniDir.getAbsolutePath(), "--flavour","m1.xlarge"});
    }

    /**
     * Test of main method, of class JobGenerator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testForcedOperation() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config");
        File iniDir = FileUtils.getFile("ini");
        JobGenerator.main(new String[] { "--config", file.getAbsolutePath(), "--ini", iniDir.getAbsolutePath(), "--force" ,
                "--flavour","m1.xlarge"});
    }

    /**
     * Test of main method, of class JobGenerator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testTestOperation() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config");
        JobGenerator.main(new String[] { "--config", file.getAbsolutePath(), "--total-jobs", "5", "--flavour","m1.xlarge"});
    }

    /**
     * Test of main method, of class JobGenerator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testTestingWithHashingOperation() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config.check_hash");
        JobGenerator.main(new String[] { "--config", file.getAbsolutePath(), "--total-jobs", "5", "--flavour","m1.xlarge"});
    }

}
