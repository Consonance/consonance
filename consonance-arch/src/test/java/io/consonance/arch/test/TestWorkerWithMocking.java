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

package io.consonance.arch.test;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import io.consonance.arch.beans.Job;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.arch.worker.WorkerRunnable;
import io.consonance.arch.worker.WorkflowRunner;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.github.collaboratory.LauncherCWL;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@PowerMockIgnore("javax.*")
@PrepareForTest({ QueueingConsumer.class, CommonServerTestUtilities.class,  CommonTestUtilities.class, WorkerRunnable.class, DefaultExecutor.class, WorkflowRunner.class,
        DefaultExecuteResultHandler.class, Logger.class, LoggerFactory.class, HierarchicalINIConfiguration.class })
@RunWith(PowerMockRunner.class)
public class TestWorkerWithMocking {

    @Mock
    private HierarchicalINIConfiguration config;

    @Mock
    private CommonServerTestUtilities mockUtil;

    @Mock
    private Channel mockChannel;

    @Mock
    private com.rabbitmq.client.Connection mockConnection;

    @Mock
    private QueueingConsumer mockConsumer;

    @Mock
    private Envelope mockEnvelope;

    @Mock
    private BasicProperties mockProperties;

    @Mock
    private HttpGet mockMethod;
    
    @Mock
    private DefaultHttpClient mockClient;

    @Mock
    private CloseableHttpResponse mockResponse;

    @Mock
    private HttpEntity mockEntity;
    
    @Spy
    private DefaultExecutor mockExecutor = new DefaultExecutor();

    private DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();

    @Mock
    private Appender<LoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> argCaptor;

    private static ch.qos.logback.classic.Logger LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CommonServerTestUtilities.class);
        PowerMockito.mockStatic(CommonTestUtilities.class);

        Mockito.when(mockAppender.getName()).thenReturn("MOCK");
        LOG.addAppender((Appender) mockAppender);

        Mockito.doNothing().when(mockConnection).close();
        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);
        Mockito.when(CommonServerTestUtilities.setupQueue(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);
        Mockito.when(CommonServerTestUtilities.setupQueueOnExchange(any(Channel.class), anyString(), anyString())).thenReturn("consonance_arch_jobs");
        Mockito.when(CommonServerTestUtilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString(), anyString())).thenReturn(mockChannel);
        Mockito.when(CommonServerTestUtilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

        StatusLine sl = new BasicStatusLine(new ProtocolVersion("HTTP",1,0), 200, "OK");
        Mockito.when(mockResponse.getStatusLine()).thenReturn(sl);
        Mockito.when(mockResponse.getEntity()).thenReturn(mockEntity);
        Mockito.when(mockEntity.getContent()).thenReturn(IOUtils.toInputStream("m3.large"));
        
        PowerMockito.whenNew(HttpGet.class).withAnyArguments().thenReturn(mockMethod);
        Mockito.when(mockClient.execute(any())).thenReturn(mockResponse);
        PowerMockito.whenNew(DefaultHttpClient.class).withAnyArguments().thenReturn(mockClient);
    }

    private String appendEventsIntoString(List<LoggingEvent> events) {
        StringBuffer sbuff = new StringBuffer();
        for (LoggingEvent e : events) {
            sbuff.append("\n").append(e.getMessage());
        }
        return sbuff.toString();
    }

    @Test
    @Ignore("broken until updated for Consonance 2.0")
    public void testRunWorker() throws Exception {

        PowerMockito.whenNew(DefaultExecuteResultHandler.class).withNoArguments().thenReturn(this.handler);
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                for (int i = 0; i < 5; i++) {
                    CommandLine cli = new CommandLine("echo");
                    cli.addArgument("iteration: " + i);
                    mockExecutor.execute(cli);
                    Thread.sleep(500);
                }
                CommandLine cli = new CommandLine("bash");
                cli.addArgument("./src/test/resources/err_output.sh");
                mockExecutor.execute(cli);
                // Here we make sure that the Handler that always gets used always returns 0, and then everything completes OK.
                handler.onProcessComplete(0);
                return null;
            }
        }).when(mockExecutor).execute(any(CommandLine.class), any(ExecuteResultHandler.class));
        PowerMockito.whenNew(DefaultExecutor.class).withNoArguments().thenReturn(mockExecutor);

        setupConfig();

        Job j = new Job();
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");

        // add new CWL stuff
        File cwlFile = FileUtils.getFile("src", "test", "resources", "collab.cwl");
        File jobFile = FileUtils.getFile("src", "test", "resources", "collab-cwl-job-pre.json");
        File engineFile = FileUtils.getFile("src", "test", "resources", "node-engine.cwl");

        j.setContainerImageDescriptor(FileUtils.readFileToString(cwlFile));
        j.setContainerRuntimeDescriptor(FileUtils.readFileToString(jobFile));
        j.getExtraFiles().put("node-engine.cwl", new Job.ExtraFile(FileUtils.readFileToString(engineFile), true));

        String json = j.toJSON();
        byte[] body = json.getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);

        testWorker.run();
        // String testResults = TestWorkerWithMocking.outBuffer.toString();// this.outStream.toString();

        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        testResults = cleanResults(testResults);
        // System.out.println("\n===============================\nTest Results: " + testResults);
        // System.out.println(testResults);
        String expectedDockerCommand = "docker run --rm -h master -t -v /var/run/docker.sock:/var/run/docker.sock -v /workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0:/workflow -v /tmp/seqware_tmpfile.ini:/ini -v /datastore:/datastore -v /home/$USER/.gnos:/home/$USER/.gnos -v /home/$USER/custom-seqware-settings:/home/seqware/.seqware/settings pancancer/seqware_whitestar_pancancer:1.1.1 seqware bundle launch --dir /workflow --ini /ini --no-metadata --engine whitestar";
        System.out.println("expected results: "+expectedDockerCommand);
        //System.out.println(testResults);
        assertTrue("Check for docker command, got " + testResults, testResults.contains(expectedDockerCommand));
        assertTrue("Check for sleep message in the following:" + testResults,
                testResults.contains("Sleeping before executing workflow for 1000 ms."));
        assertTrue("Check for workflow complete", testResults.contains("Docker execution result: \"iteration: 0\"\n" + "\"iteration: 1\"\n"
                + "\"iteration: 2\"\n" + "\"iteration: 3\"\n" + "\"iteration: 4\"\n"));

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output:" + StringUtils.difference(begining, testResults), testResults.contains(begining));

        assertTrue("check INI: " + testResults, testResults.contains("param1=value1") && testResults.contains("param2=value2")
                && testResults.contains("param3=help I'm trapped in an INI file"));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        String initalHeartbeat = new String(Files.readAllBytes(Paths.get("src/test/resources/testInitialHeartbeat.txt")));
        assertTrue("Check for an initial heart beat, found" + testResults, testResults.contains(initalHeartbeat));

        assertTrue("check for stderr in heartbeat", testResults.contains("\"stderr\": \"123_err\","));
    }

    private void setupConfig() {
        // need launcher CWL
        Mockito.when(config.getString(LauncherCWL.WORKING_DIRECTORY)).thenReturn("./datastore/");

        Mockito.when(config.getString(Constants.RABBIT_QUEUE_NAME)).thenReturn("consonance_arch");
        Mockito.when(config.getString(Constants.RABBIT_HOST)).thenReturn("localhost");
        Mockito.when(config.getString(Constants.RABBIT_USERNAME)).thenReturn("guest");
        Mockito.when(config.getString(Constants.RABBIT_PASSWORD)).thenReturn("guest");

        Mockito.when(config.getString(Constants.WORKER_HEARTBEAT_RATE)).thenReturn("2.5");
        Mockito.when(config.getString(Constants.WORKER_MAX_RUNS)).thenReturn("1");
        Mockito.when(config.getLong(Constants.WORKER_PREWORKER_SLEEP, WorkerRunnable.DEFAULT_PRESLEEP)).thenReturn(1L);
        Mockito.when(config.getLong(Constants.WORKER_POSTWORKER_SLEEP, WorkerRunnable.DEFAULT_POSTSLEEP)).thenReturn(1L);
        Mockito.when(config.getString(Constants.WORKER_ENDLESS)).thenReturn("1");

        Mockito.when(config.getString(Constants.WORKER_SEQWARE_ENGINE, Constants.SEQWARE_WHITESTAR_ENGINE)).thenReturn(
                Constants.SEQWARE_WHITESTAR_ENGINE);
        Mockito.when(config.getString(Constants.WORKER_SEQWARE_SETTINGS_FILE)).thenReturn("/home/ubuntu/custom-seqware-settings");

        Mockito.when(config.getString(Constants.WORKER_HOST_USER_NAME, "ubuntu")).thenReturn("ubuntu");
        Mockito.when(CommonTestUtilities.parseConfig(anyString())).thenReturn(config);
    }

    private String cleanResults(String testResults) {
        testResults = testResults.replaceAll("/home/ubuntu/", "/home/\\$USER/");
        testResults = testResults.replaceAll("seqware_[0-9]+\\.ini", "seqware_tmpfile.ini");
        testResults = testResults.replaceAll("oozie-[a-z0-9\\-]+", "JOB_ID");
        testResults = testResults.replaceAll("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}", "0000/00/00 00:00:00");
        testResults = testResults.replaceAll("bundle_manager\\d+", "bundle_manager_LONG_NUMERIC_SEQUENCE");
        testResults = testResults.replaceAll("scheduler\\d+out", "schedulerLONG_NUMERIC_SEQUENCEout");
        testResults = testResults.replaceAll("IP address: /[^\"]*", "IP address: 0.0.0.0");
        return testResults;
    }
}
