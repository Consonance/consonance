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
package io.consonance.arch.worker;

import java.io.File;
import java.net.InetAddress;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class WorkerRunnableIT {

    /**
     * Test of getFirstNonLoopbackAddress method, of class WorkerRunnable.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetFirstNonLoopbackAddress() throws Exception {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        WorkerRunnable instance = new WorkerRunnable(configFile.getAbsolutePath(), "test", 1);
        InetAddress result = instance.getFirstNonLoopbackAddress();
        Assert.assertTrue("ip address was null", result != null);
    }

}
