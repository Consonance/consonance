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
package info.consonance.arch.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import info.consonance.arch.coordinator.Coordinator;
import info.consonance.arch.jobGenerator.JobGenerator;
import info.consonance.arch.utils.ITUtilities;

/**
 *
 * @author dyuen
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore( {"javax.management.*"}) 
public class WorkerIT {

//    @BeforeClass
//    public static void setup() throws IOException, TimeoutException {
//        ITUtilities.clearState();
//    }

    @Mock
    private GetMethod mockMethod;
    
    @Mock
    private HttpClient mockClient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ITUtilities.clearState();
    }
    
    /**
     * Test of main method, of class JobGenerator.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testTestModeOperation() throws Exception {
        File file = FileUtils.getFile("src", "test", "resources", "config");
        File iniDir = FileUtils.getFile("ini");

        //Need to mock HTTP responses because the unit tests may not run in a place where there is a metadata service available.
        StatusLine sl = new StatusLine("HTTP/1.0 200 OK");
        Mockito.when(mockMethod.getStatusLine()).thenReturn(sl);
        Mockito.when(mockMethod.getResponseBodyAsString()).thenReturn("m3.large");
        
        //PowerMockito.whenNew(GetMethod.class).withArguments("http://169.254.169.254/latest/meta-data/instance-type").thenReturn((GetMethod) mockMethod);
        PowerMockito.whenNew(GetMethod.class).withArguments(anyString()).thenReturn((GetMethod) mockMethod);
        Mockito.when(mockClient.executeMethod(any())).thenReturn(new Integer(200));
        PowerMockito.whenNew(HttpClient.class).withNoArguments().thenReturn(mockClient);

        
        // prime the coordinator with an order
        JobGenerator
                .main(new String[] { "--config", file.getAbsolutePath(), "--ini", iniDir.getAbsolutePath(), "--workflow-name", "DEWrapper",
                        "--workflow-version", "1.0.0", "--workflow-path",
                        "/workflows/Workflow_Bundle_DEWrapperWorkflow_1.0.0_SeqWare_1.1.0" ,
                        "--flavour","m1.xlarge"});
        // prime the worker with a job
        Coordinator.main(new String[] { "--config", file.getAbsolutePath() });
        
        Worker.main(new String[] { "--config", file.getAbsolutePath(), "--uuid", "12345", "--test", "--pidFile",
                "/var/run/arch3_worker.pid" ,"--flavour", "cherry"});
    }

}
