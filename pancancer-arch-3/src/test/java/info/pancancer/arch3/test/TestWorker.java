package info.pancancer.arch3.test;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;
import info.pancancer.arch3.worker.WorkerHeartbeat;
import info.pancancer.arch3.worker.WorkerRunnable;
import info.pancancer.arch3.worker.WorkflowResult;
import info.pancancer.arch3.worker.WorkflowRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PrepareForTest({ QueueingConsumer.class, Worker.class, WorkerRunnable.class, Utilities.class, WorkerHeartbeat.class, WorkflowRunner.class,
        Appender.class, Logger.class, LoggerFactory.class, ch.qos.logback.classic.Logger.class })
@RunWith(PowerMockRunner.class)
public class TestWorker {

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

    @Before
    public void setup() throws IOException, Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(mockAppender.getName()).thenReturn("MOCK");
        LOG.addAppender((Appender) mockAppender);
        PowerMockito.mockStatic(Utilities.class);
        Mockito.doNothing().when(mockConnection).close();
        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);
        Mockito.when(Utilities.setupQueue(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);
        Mockito.when(Utilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

        WorkflowResult result = new WorkflowResult();
        result.setWorkflowStdout("Mock Workflow Response");
        result.setWorkflowStdErr("Mock Workflow Response");
        result.setExitCode(0);
        Mockito.when(mockRunner.call()).thenReturn(result);
        PowerMockito.whenNew(WorkflowRunner.class).withNoArguments().thenReturn(mockRunner);

        // Always return this heartbeat object.
        PowerMockito.whenNew(WorkerHeartbeat.class).withNoArguments().thenReturn(heartbeat);
    }

    @Test
    public void testWorker_noQueueName() {
        PowerMockito.mockStatic(Utilities.class);
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(new HierarchicalINIConfiguration());
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
    public void testWorker_exception() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        Mockito.when(mockRunner.call()).thenThrow(new RuntimeException("Mock Exception"));
        PowerMockito.whenNew(WorkflowRunner.class).withNoArguments().thenReturn(mockRunner);

        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);

        testWorker.run();
        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        assertTrue("Mock Exception is present", testResults.contains("java.lang.RuntimeException: Mock Exception"));
    }

    @Test
    public void testWorker_emptyMessage() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();
        byte body[] = ("").getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);
        testWorker.run();
        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
    }

    @Test
    public void testWorker_nullMessage() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();

        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, null);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);
        try {
            testWorker.run();
            Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
            List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
            String testResults = this.appendEventsIntoString(tmpList);

            assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String appendEventsIntoString(List<LoggingEvent> events) {
        StringBuffer sbuff = new StringBuffer();
        for (LoggingEvent e : events) {
            sbuff.append(e.getMessage());
        }
        return sbuff.toString();
    }

    @Test
    public void testWorker_endless() throws Exception {

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);
        Mockito.when(Utilities.parseJSONStr(anyString())).thenCallRealMethod();
        Mockito.when(Utilities.parseConfig(anyString())).thenCallRealMethod();
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
                    Thread.sleep(500);
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
        configObj.addProperty("rabbit.rabbitMQQueueName", "seqware");
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
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(configObj);
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
                    Thread.sleep(500);
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
    public void testWorker_main() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
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
    public void testWorker_mainNoArgs() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
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
    public void testWorker_mainUUIDOonly() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
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

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        // System.out.println(testResults);
        assertTrue("check begining of output", testResults.contains(begining));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        assertTrue("Check for \"docker\" command", testResults.contains("Docker execution result: Mock Workflow Response"));
    }

    @Test
    public void testWorker() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resource/workerConfig.ini", "vm123456", 1);

        testWorker.run();

        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);
        testResults = cleanResults(testResults);

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output", testResults.contains(begining));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        
        System.out.println(ending);
        System.out.println(testResults);
        assertTrue("check ending of output", testResults.contains(ending));

        assertTrue("Check for \"docker\" command", testResults.contains("Docker execution result: Mock Workflow Response"));
    }

    private byte[] setupMessage() {
        Job j = new Job();
        j.setWorkflowPath("/workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0");
        j.setWorkflow("HelloWorld");
        j.setWorkflowVersion("1.0-SNAPSHOT");
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");
        Map<String, String> iniMap = new HashMap<>(3);
        iniMap.put("param1", "value1");
        iniMap.put("param2", "value2");
        iniMap.put("param3", "help I'm trapped in an INI file");
        j.setIni(iniMap);
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

        configObj.addProperty("rabbit.rabbitMQQueueName", "seqware");
        configObj.addProperty("worker.heartbeatRate", "2.5");
        configObj.addProperty("worker.preworkerSleep", "1");
        configObj.addProperty("worker.postworkerSleep", "1");
        configObj.addProperty("worker.hostUserName", System.getProperty("user.name"));
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(configObj);
    }

    private void setupMockQueue(Delivery testDelivery) throws InterruptedException, Exception {
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);
    }
}
