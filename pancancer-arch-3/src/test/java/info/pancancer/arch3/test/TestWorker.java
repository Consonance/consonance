package info.pancancer.arch3.test;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
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
        Logger.class, LoggerFactory.class })
@RunWith(PowerMockRunner.class)
public class TestWorker {

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

        PowerMockito.mockStatic(Utilities.class);

        Mockito.doNothing().when(mockConnection).close();

        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);

        Mockito.when(Utilities.setupQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        Mockito.when(Utilities.setupExchange(any(JSONObject.class), anyString())).thenReturn(mockChannel);

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
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(new JSONObject());
        try {
            WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.json", "vm123456", 1);
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

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.json", "vm123456", 1);

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

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.json", "vm123456", 1);
        testWorker.run();
        String testResults = outBuffer.toString();
        assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
    }

    @Test
    public void testWorker_nullMessage() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();

        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, null);
        setupMockQueue(testDelivery);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.json", "vm123456", 1);
        try {
            testWorker.run();
            String testResults = outBuffer.toString();
            assertTrue("empty message warning", testResults.contains(" [x] Job request came back null/empty! "));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWorker_main() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        setupConfig();

        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Worker.main(new String[] { "--config", "src/test/resources/workerConfig.json", "--uuid", "vm123456", "--max-runs", "1",
                "--pidFile", "/var/run/arch3_worker.pid" });

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
                "src/test/resources/workerConfig.json" });

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

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.json", "vm123456", 1);

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

    private void setupConfig() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("rabbitMQQueueName", "seqware");
        jsonObj.put("heartbeatRate", "2.5");
        jsonObj.put("preworkerSleep", "1");
        jsonObj.put("postworkerSleep", "1");
        jsonObj.put("hostUserName", System.getProperty("user.name"));
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(jsonObj);
    }

    private void setupMockQueue(Delivery testDelivery) throws InterruptedException, Exception {
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);
    }
}
