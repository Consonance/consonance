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

package io.consonance.arch.worker;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import io.consonance.arch.beans.Job;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@PrepareForTest({ QueueingConsumer.class, Worker.class, WorkerRunnable.class, CommonServerTestUtilities.class, CommonTestUtilities.class, WorkerHeartbeat.class, WorkflowRunner.class,
        Appender.class, Logger.class, LoggerFactory.class, ch.qos.logback.classic.Logger.class })
@RunWith(PowerMockRunner.class)
public class MockingWorkerIT {

    public static final int EIGHT_SECONDS = 8000;
    private static ch.qos.logback.classic.Logger LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private WorkerHeartbeat heartbeat = new WorkerHeartbeat();

    @Mock
    private WorkflowRunner mockRunner;

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
    private Appender<LoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> argCaptor;
    
    @Mock
    private HttpGet mockMethod;

    @Mock
    private CloseableHttpResponse mockResponse;

    @Mock
    private HttpEntity mockEntity;
    
    @Mock
    private DefaultHttpClient mockClient;
    
    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(mockAppender.getName()).thenReturn("MOCK");
        LOG.addAppender((Appender) mockAppender);
        PowerMockito.mockStatic(CommonServerTestUtilities.class);
        PowerMockito.mockStatic(CommonTestUtilities.class);
        Mockito.doNothing().when(mockConnection).close();
        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);
        Mockito.when(CommonServerTestUtilities.setupQueue(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);
        Mockito.when(CommonServerTestUtilities.setupQueueOnExchange(any(Channel.class), anyString(), anyString())).thenReturn("consonance_arch_jobs");
        Mockito.when(CommonServerTestUtilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString(), anyString())).thenReturn(mockChannel);
        Mockito.when(CommonServerTestUtilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

        WorkflowResult result = new WorkflowResult();
        result.setWorkflowStdout("Mock Workflow Response");
        result.setWorkflowStdErr("Mock Workflow Response");
        result.setExitCode(0);
        Mockito.when(mockRunner.call()).thenReturn(result);
        PowerMockito.whenNew(WorkflowRunner.class).withNoArguments().thenReturn(mockRunner);

        // Always return this heartbeat object.
        PowerMockito.whenNew(WorkerHeartbeat.class).withNoArguments().thenReturn(heartbeat);
        
        StatusLine sl = new BasicStatusLine(new ProtocolVersion("HTTP",1,0), 200, "OK");
        Mockito.when(mockResponse.getStatusLine()).thenReturn(sl);
        Mockito.when(mockResponse.getEntity()).thenReturn(mockEntity);
        Mockito.when(mockEntity.getContent()).thenReturn(IOUtils.toInputStream("m3.large"));


        PowerMockito.whenNew(HttpGet.class).withAnyArguments().thenReturn(mockMethod);
        Mockito.when(mockClient.execute(any())).thenReturn(mockResponse);
        PowerMockito.whenNew(DefaultHttpClient.class).withAnyArguments().thenReturn(mockClient);
    }

    @Test
    public void testWorker_noQueueName() {
        PowerMockito.mockStatic(CommonServerTestUtilities.class);
        Mockito.when(CommonTestUtilities.parseConfig(anyString())).thenReturn(new HierarchicalINIConfiguration());
        try {
            WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);
            fail("Execution should not have reached this point!");
        } catch (Exception e) {
            assertTrue(
                    "Queue Name message",
                    e.getMessage()
                            .contains(
                                    "Queue name was null! Please ensure that you have properly configured \"rabbitMQQueueName\" in your config file."));
        }
    }

    @Test
    public void testWorker_exception() throws Exception {
        Mockito.when(mockRunner.call()).thenThrow(new RuntimeException("Mock Exception"));
        PowerMockito.whenNew(WorkflowRunner.class).withNoArguments().thenReturn(mockRunner);

        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);

        testWorker.run();
        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        assertTrue("Mock Exception is present", testResults.contains("java.lang.RuntimeException: Mock Exception"));
    }

    @Test
    public void testWorker_emptyMessage() throws Exception {
        setupConfig();
        byte body[] = ("").getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);
        testWorker.run();
        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
    }

    @Test
    public void testWorker_nullMessage() throws Exception {
        setupConfig();

        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, null);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);
        try {
            testWorker.run();
            Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
            List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
            String testResults = this.appendEventsIntoString(tmpList);

            System.out.println(testResults);
            assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String appendEventsIntoString(List<LoggingEvent> events) {
        StringBuffer sbuff = new StringBuffer();
        for (LoggingEvent e : events) {
            // why does this become null now?
            sbuff.append(e != null ? e.getMessage() : "");
        }
        return sbuff.toString();
    }

    @Test
    public void testWorker_endless() throws Exception {

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);
        Mockito.when(CommonServerTestUtilities.parseJSONStr(anyString())).thenCallRealMethod();
        Mockito.when(CommonTestUtilities.parseConfig(anyString())).thenCallRealMethod();
        final FutureTask<String> tester = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() {
                LOG.debug("tester thread started");
                try {
                    Worker.main(new String[] { "--config", "src/test/resources/workerConfig.ini", "--uuid", "vm123456", "--endless",
                            "--pidFile", "/var/run/arch3_worker.pid" });
                } catch (CancellationException | InterruptedException e) {
                    LOG.error("Exception caught: " + e.getMessage());
                    return e.getMessage();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                    return null;
                } finally {
                    Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
                    String s = appendEventsIntoString(argCaptor.getAllValues());
                    return s;
                }
            }

        });

        final Thread killer = new Thread(new Runnable() {

            @Override
            public void run() {
                LOG.debug("killer thread started");
                try {
                    // The endless worker will not end on its own (because it's endless) so we need to wait a little bit (0.5 seconds) and
                    // then kill it as if it were killed by the command-line script (kill_worker_daemon.sh).
                    Thread.sleep(EIGHT_SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LOG.error(e.getMessage());
                }
                tester.cancel(true);
            }
        });

        ExecutorService es = Executors.newFixedThreadPool(2);
        es.execute(tester);
        es.execute(killer);
        try {
            tester.get();
        } catch (CancellationException e) {
            Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
            List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
            String output = this.appendEventsIntoString(tmpList);
            assertTrue(output.contains("The \"--endless\" flag was set, this worker will run endlessly!"));

            int numJobsPulled = StringUtils.countMatches(output, " WORKER IS PREPARING TO PULL JOB FROM QUEUE ");

            LOG.info("Number of jobs attempted: " + numJobsPulled);
            assertTrue("number of jobs attempted > 1", numJobsPulled > 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testWorker_endlessFromConfig() throws Exception {
        HierarchicalINIConfiguration configObj = new HierarchicalINIConfiguration();
        configObj.addProperty("rabbit.rabbitMQQueueName", "consonance_arch");
        configObj.addProperty("rabbit.rabbitMQHost", "localhost");
        configObj.addProperty("rabbit.rabbitMQUser", "guest");
        configObj.addProperty("rabbit.rabbitMQPass", "guest");
        configObj.addProperty("worker.heartbeatRate", "2.5");
        configObj.addProperty("worker.max-runs", "1");
        configObj.addProperty("worker.preworkerSleep", "1");
        configObj.addProperty("worker.postworkerSleep", "1");
        configObj.addProperty("worker.endless", "true");
        configObj.addProperty("worker.hostUserName", System.getProperty("user.name"));

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);
        Mockito.when(CommonTestUtilities.parseConfig(anyString())).thenReturn(configObj);
        final FutureTask<String> tester = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() {
                LOG.info("tester thread started");
                try {
                    Worker.main(new String[] { "--config", "src/test/resources/workerConfig.ini", "--uuid", "vm123456", "--pidFile",
                            "/var/run/arch3_worker.pid" });
                } catch (CancellationException | InterruptedException e) {
                    LOG.error("Exception caught: " + e.getMessage());
                    return e.getMessage();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                    return null;
                } finally {
                    Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
                    String s = appendEventsIntoString(argCaptor.getAllValues());
                    return s;
                }
            }

        });

        final Thread killer = new Thread(new Runnable() {

            @Override
            public void run() {
                LOG.info("killer thread started");
                try {
                    // The endless worker will not end on its own (because it's endless) so we need to wait a little bit (0.5 seconds) and
                    // then kill it as if it were killed by the command-line script (kill_worker_daemon.sh).
                    Thread.sleep(EIGHT_SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LOG.error(e.getMessage());
                }
                tester.cancel(true);
            }
        });

        ExecutorService es = Executors.newFixedThreadPool(2);
        es.execute(tester);
        es.execute(killer);

        try {
            tester.get();
        } catch (CancellationException e) {
            Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
            List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
            String output = this.appendEventsIntoString(tmpList);

            assertTrue("--endless flag was detected and set",
                    output.contains("The \"--endless\" flag was set, this worker will run endlessly!"));
            int numJobsPulled = StringUtils.countMatches(output, " WORKER IS PREPARING TO PULL JOB FROM QUEUE ");
            LOG.info("Number of jobs attempted: " + numJobsPulled);
            assertTrue("number of jobs attempted > 1", numJobsPulled > 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testWorker_main() throws Exception {
        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Worker.main(new String[] { "--config", "src/test/resources/workerConfig.ini", "--uuid", "vm123456", "--max-runs", "1", "--pidFile",
                "/var/run/arch3_worker.pid" });

        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        testResults = cleanResults(testResults);

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output: " + testResults, testResults.contains(begining));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        assertTrue("Check for \"docker\" command", testResults.contains("Docker execution result: Mock Workflow Response"));
    }

    @Test
    public void testWorker_mainNoArgs() throws Exception {
        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        try {
            Worker.main(new String[] {});
            fail("this line should not have been reached");
        } catch (Exception e) {
            LOG.error(e.getMessage());
            assertTrue(e.getMessage().contains("Missing required option(s) [config, uuid]"));
        }

    }

    @Test
    public void testWorker_mainUUIDOonly() throws Exception {
        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Worker.main(new String[] { "--uuid", "vm123456", "--pidFile", "/var/run/arch3_worker.pid", "--config",
                "src/test/resources/workerConfig.ini" });

        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        testResults = cleanResults(testResults);

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt"))).trim();
        // System.out.println(testResults);
        assertTrue("check begining of output: " + testResults, testResults.contains(begining));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        assertTrue("Check for \"docker\" command", testResults.contains("Docker execution result: Mock Workflow Response"));
    }

    @Test
    public void testWorker() throws Exception {
        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resource/workerConfig.ini", "vm123456", 1);

        testWorker.run();

        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);
        testResults = cleanResults(testResults);

        String beginning = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check beginning of output", testResults.contains(beginning));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        assertTrue("Check for \"docker\" command", testResults.contains("Docker execution result: Mock Workflow Response"));
    }

    private byte[] setupMessage() {
        Job j = new Job();
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");
        byte[] body = j.toJSON().getBytes();
        return body;
    }

    private String cleanResults(String testResults) {
        testResults = testResults.replaceAll("seqware_[0-9]+\\.ini", "seqware_tmpfile.ini");
        testResults = testResults.replaceAll("oozie-[a-z0-9\\-]+", "JOB_ID");
        testResults = testResults.replaceAll("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}", "0000/00/00 00:00:00");
        testResults = testResults.replaceAll("bundle_manager\\d+", "bundle_manager_LONG_NUMERIC_SEQUENCE");
        testResults = testResults.replaceAll("scheduler\\d+out", "schedulerLONG_NUMERIC_SEQUENCEout");
        testResults = testResults.replaceAll("\r", "");
        testResults = testResults.replaceAll("IP address: /[^\"]*", "IP address: 0.0.0.0");
        testResults = testResults.replaceAll("/home/[^ /]*/", "/home/\\$USER/");
        return testResults;
    }

    private void setupConfig() {
        HierarchicalINIConfiguration configObj = new HierarchicalINIConfiguration();

        configObj.addProperty("rabbit.rabbitMQQueueName", "consonance_arch");
        configObj.addProperty("worker.heartbeatRate", "2.5");
        configObj.addProperty("worker.preworkerSleep", "1");
        configObj.addProperty("worker.postworkerSleep", "1");
        configObj.addProperty("worker.hostUserName", System.getProperty("user.name"));
        Mockito.when(CommonTestUtilities.parseConfig(anyString())).thenReturn(configObj);
    }

    private void setupMockQueue(Delivery testDelivery) throws Exception {
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);
    }
}
