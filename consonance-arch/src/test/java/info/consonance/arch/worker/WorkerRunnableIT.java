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

import java.io.File;
import java.net.InetAddress;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;

/**
 *
 * @author dyuen
 */
public class WorkerRunnableIT {

    @Mock
    private GetMethod mockMethod;
    
    @Mock
    private HttpClient mockClient;
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }
    
    /**
     * Test of getFirstNonLoopbackAddress method, of class WorkerRunnable.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetFirstNonLoopbackAddress() throws Exception {
        StatusLine sl = new StatusLine("HTTP/1.0 200 OK");
        Mockito.when(mockMethod.getStatusLine()).thenReturn(sl);
        Mockito.when(mockMethod.getResponseBodyAsString()).thenReturn("m3.large");
        
        PowerMockito.whenNew(GetMethod.class).withAnyArguments().thenReturn((GetMethod) mockMethod);
        Mockito.when(mockClient.executeMethod(any())).thenReturn(new Integer(200));
        PowerMockito.whenNew(HttpClient.class).withNoArguments().thenReturn(mockClient);

        
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        WorkerRunnable instance = new WorkerRunnable(configFile.getAbsolutePath(), "test", 1);
        InetAddress result = instance.getFirstNonLoopbackAddress();
        Assert.assertTrue("ip address was null", result != null);
    }

}
