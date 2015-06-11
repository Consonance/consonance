package info.pancancer.arch3.test;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;
import info.pancancer.arch3.worker.WorkerHeartbeat;
import info.pancancer.arch3.worker.WorkerRunnable;
import info.pancancer.arch3.worker.WorkflowResult;
import info.pancancer.arch3.worker.WorkflowRunner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PrepareForTest({ QueueingConsumer.class, Worker.class, WorkerRunnable.class, Utilities.class, WorkerHeartbeat.class, WorkflowRunner.class,
        Logger.class, LoggerFactory.class, HierarchicalINIConfiguration.class })
@RunWith(PowerMockRunner.class)
public class TestWorker {

    @Mock
    private HierarchicalINIConfiguration config;

    @Mock
    private WorkerHeartbeat mockHeartbeat;

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
    private Logger mockLogger;

    private static StringBuffer outBuffer = new StringBuffer();

    public class LoggingAnswer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            outBuffer.append(invocation.getArguments()[0]).append("\n");
            if (invocation.getArguments().length == 2) {
                if (invocation.getArguments()[1] instanceof Throwable) {
                    OutputStream outStream = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(outStream);
                    ((Throwable) invocation.getArguments()[1]).printStackTrace(ps);
                    outBuffer.append(new String(outStream.toString()));
                    ps.close();
                    outStream.close();
                }
            }
            return null;
        }
    }

    private LoggingAnswer logAnswer = new LoggingAnswer();

    @Before
    public void setup() throws IOException, Exception {
        MockitoAnnotations.initMocks(this);

        outBuffer = new StringBuffer();
        Mockito.doAnswer(logAnswer).when(mockLogger).info(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).debug(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).error(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).error(anyString(), any(Exception.class));
        PowerMockito.mockStatic(org.slf4j.LoggerFactory.class);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(Worker.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(WorkerRunnable.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(WorkerHeartbeat.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(WorkflowRunner.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(Utilities.class)).thenReturn(mockLogger);

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

        Mockito.doNothing().when(mockHeartbeat).run();
        PowerMockito.whenNew(WorkerHeartbeat.class).withNoArguments().thenReturn(mockHeartbeat);
    }

    @Test
    public void testWorker_noQueueName() {
        PowerMockito.mockStatic(Utilities.class);
        setupConfig(false);

        try {
            WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig", "vm123456", 1);
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

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig", "vm123456", 1);

        testWorker.run();
        String testResults = outBuffer.toString();

        assertTrue("Mock Exception is present", testResults.contains("java.lang.RuntimeException: Mock Exception"));
    }

    @Test
    public void testWorker_emptyMessage() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();
        byte body[] = ("").getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig", "vm123456", 1);
        testWorker.run();
        String testResults = outBuffer.toString();
        assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
    }

    @Test
    public void testWorker_nullMessage() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();

        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, null);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig", "vm123456", 1);
        try {
            testWorker.run();
            String testResults = outBuffer.toString();
            assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWorker_endless() throws Exception {

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);
        Mockito.when(Utilities.parseJSONStr(anyString())).thenCallRealMethod();
        Mockito.when(Utilities.parseConfig(anyString())).thenCallRealMethod();
        final FutureTask<String> tester = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                System.out.println("tester thread started");
                try {
                    Worker.main(new String[] { "--config", "src/test/resources/workerConfig", "--uuid", "vm123456", "--endless",
                            "--pidFile", "/var/run/arch3_worker.pid" });
                } catch (CancellationException | InterruptedException e) {
                    String s = outBuffer.toString();
                    System.out.println("Exception caught: " + e.getMessage());
                    return s;
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                    return null;
                } finally {
                    return outBuffer.toString();
                }
            }

        });

        final Thread killer = new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("killer thread started");
                try {
                    // The endless worker will not end on its own (because it's endless) so we need to wait a little bit (0.5 seconds) and
                    // then kill it as if it were killed by the command-line script (kill_worker_daemon.sh).
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
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
            // System.out.println(outBuffer.toString());
            String output = outBuffer.toString();

            assertTrue("--endless flag was detected and set",
                    output.contains("The \"--endless\" flag was set, this worker will run endlessly!"));
            int numJobsPulled = StringUtils.countMatches(output, " WORKER IS PREPARING TO PULL JOB FROM QUEUE ");
            System.out.println("Number of jobs attempted: " + numJobsPulled);
            assertTrue("number of jobs attempted > 1", numJobsPulled > 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testWorker_endlessFromConfig() throws Exception {

        Mockito.when(config.getString(Constants.RABBIT_QUEUE_NAME)).thenReturn("seqware");
        Mockito.when(config.getString(Constants.RABBIT_HOST)).thenReturn("localhost");
        Mockito.when(config.getString(Constants.RABBIT_USERNAME)).thenReturn("guest");
        Mockito.when(config.getString(Constants.RABBIT_PASSWORD)).thenReturn("guest");

        Mockito.when(config.getString(Constants.WORKER_HEARTBEAT_RATE)).thenReturn("2.5");
        Mockito.when(config.getString(Constants.WORKER_MAX_RUNS)).thenReturn("1");
        Mockito.when(config.getString(Constants.WORKER_PREWORKER_SLEEP)).thenReturn("1");
        Mockito.when(config.getString(Constants.WORKER_POSTWORKER_SLEEP)).thenReturn("1");
        Mockito.when(config.getString(Constants.WORKER_ENDLESS)).thenReturn("1");

        Mockito.when(config.getString(Constants.WORKER_HOST_USER_NAME)).thenReturn(System.getProperty("user.name"));
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(config);

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(config);
        final FutureTask<String> tester = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                System.out.println("tester thread started");
                try {
                    Worker.main(new String[] { "--config", "src/test/resources/workerConfig", "--uuid", "vm123456", "--pidFile",
                            "/var/run/arch3_worker.pid" });
                } catch (CancellationException | InterruptedException e) {
                    String s = outBuffer.toString();
                    System.out.println("Exception caught: " + e.getMessage());
                    return s;
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                    return null;
                } finally {
                    return outBuffer.toString();
                }
            }

        });

        final Thread killer = new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("killer thread started");
                try {
                    // The endless worker will not end on its own (because it's endless) so we need to wait a little bit (0.5 seconds) and
                    // then kill it as if it were killed by the command-line script (kill_worker_daemon.sh).
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
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
            String output = outBuffer.toString();
            assertTrue("--endless flag was detected and set",
                    output.contains("The \"--endless\" flag was set, this worker will run endlessly!"));
            int numJobsPulled = StringUtils.countMatches(output, " WORKER IS PREPARING TO PULL JOB FROM QUEUE ");
            System.out.println("Number of jobs attempted: " + numJobsPulled);
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

        Worker.main(new String[] { "--config", "src/test/resources/workerConfig", "--uuid", "vm123456", "--max-runs", "1", "--pidFile",
                "/var/run/arch3_worker.pid" });

        String testResults = outBuffer.toString();

        testResults = cleanResults(testResults);

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output", testResults.contains(begining));

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
            System.out.println(e.getMessage());
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
                "src/test/resources/workerConfig" });

        String testResults = outBuffer.toString();

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

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig", "vm123456", 1);

        testWorker.run();

        String testResults = outBuffer.toString();
        testResults = cleanResults(testResults);

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output", testResults.contains(begining));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
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

    private HierarchicalINIConfiguration setupConfig() {
        return setupConfig(true);
    }

    private HierarchicalINIConfiguration setupConfig(boolean properQueueName) {
        if (properQueueName) {
            Mockito.when(config.getString(Constants.RABBIT_QUEUE_NAME)).thenReturn("seqware");
        }
        Mockito.when(config.getString(Constants.WORKER_HEARTBEAT_RATE)).thenReturn("2.5");
        Mockito.when(config.getString(Constants.WORKER_PREWORKER_SLEEP)).thenReturn("1");
        Mockito.when(config.getString(Constants.WORKER_POSTWORKER_SLEEP)).thenReturn("1");
        Mockito.when(config.getString(Constants.WORKER_HOST_USER_NAME)).thenReturn(System.getProperty("user.name"));
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(config);
        return config;
    }

    private void setupMockQueue(Delivery testDelivery) throws InterruptedException, Exception {
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);
    }
}
