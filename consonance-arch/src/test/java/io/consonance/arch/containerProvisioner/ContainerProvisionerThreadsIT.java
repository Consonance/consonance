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
package io.consonance.arch.containerProvisioner;

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
public class ContainerProvisionerThreadsIT {

    @BeforeClass
    public static void setup() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
    }

    /**
     * Test of main method, of class ContainerProvisionerThreads.
     *
     * @throws java.lang.Exception
     */
    @Test(expected = OptionException.class)
    public void testHelpMessage() throws Exception {
        ContainerProvisionerThreads.main(new String[] { "--help" });
    }

    /**
     * Test of main method, of class ContainerProvisionerThreads.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testTestModeOperation() throws Exception {
        // ideally, we would mock up some orders here
        File file = FileUtils.getFile("src", "test", "resources", "config");
        ContainerProvisionerThreads.main(new String[] { "--test", "--config", file.getAbsolutePath() });
    }

}
