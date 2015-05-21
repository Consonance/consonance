package info.pancancer.arch3.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.CollectingLogOutputStream;
import info.pancancer.arch3.worker.WorkerRunnable;
import info.pancancer.arch3.worker.WorkerHeartbeat;
import info.pancancer.arch3.worker.WorkflowRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;

@PrepareForTest({ QueueingConsumer.class, Utilities.class, WorkerRunnable.class, DefaultExecutor.class, WorkflowRunner.class,
        DefaultExecuteResultHandler.class, Logger.class, LoggerFactory.class })
@RunWith(PowerMockRunner.class)
public class TestWorkerWithMockingIT {

    @Mock
    private Utilities mockUtil;

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

    @Spy
    private DefaultExecutor mockExecutor = new DefaultExecutor();

    private DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();

    // private ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    // private PrintStream originalOutStream = new PrintStream(System.out);
    // private PrintStream testPrintStream = new PrintStream(outStream);

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
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Utilities.class);
        // System.setOut(testPrintStream);

        outBuffer = new StringBuffer();
        Mockito.doAnswer(logAnswer).when(mockLogger).info(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).debug(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).error(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).error(anyString(), any(Exception.class));
        PowerMockito.mockStatic(org.slf4j.LoggerFactory.class);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(WorkerRunnable.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(Utilities.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(WorkerHeartbeat.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(WorkflowRunner.class)).thenReturn(mockLogger);

        Mockito.doNothing().when(mockConnection).close();
        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);
        Mockito.when(Utilities.setupQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);
        Mockito.when(Utilities.setupExchange(any(JSONObject.class), anyString())).thenReturn(mockChannel);
    }

    @Test
    public void testRunWorker() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {
        // PumpStreamHandler streamHandler = new PumpStreamHandler(new CollectingLogOutputStream());
        // mockExecutor.setStreamHandler(streamHandler);
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

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("rabbitMQQueueName", "seqware");
        jsonObj.put("heartbeatRate", "2.5");
        jsonObj.put("preworkerSleep", "1");
        jsonObj.put("postworkerSleep", "1");
        jsonObj.put("hostUserName", System.getProperty("user.name"));
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(jsonObj);

        Job j = new Job();
        j.setWorkflowPath("/workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0");
        j.setWorkflow("HelloWorld");
        j.setWorkflowVersion("1.0-SNAPSHOT");
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");
        Map<String, String> iniMap = new HashMap<String, String>(3);
        iniMap.put("param1", "value1");
        iniMap.put("param2", "value2");
        iniMap.put("param3", "help I'm trapped in an INI file");
        j.setIni(iniMap);
        byte[] body = j.toJSON().getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.json", "vm123456", 1);

        testWorker.run();
        String testResults = this.outBuffer.toString();// this.outStream.toString();
        // String knownResults = new String(Files.readAllBytes(Paths.get("src/test/resources/TestWorkerResult.txt")));
        // System.setOut(originalOutStream);

        testResults = cleanResults(testResults);
        System.out.println("\n===============================\nTest Results: " + testResults);
        assertTrue(
                "Check for docker command",
                testResults
                        .contains("Executing command: [docker run --rm -h master -t -v /var/run/docker.sock:/var/run/docker.sock -v /workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0:/workflow -v /tmp/seqware_tmpfile.ini:/ini -v /datastore:/datastore -v /home/$USER/.ssh/gnos.pem:/home/$USER/.ssh/gnos.pem seqware/seqware_whitestar_pancancer seqware bundle launch --dir /workflow --ini /ini --no-metadata]"));
        assertTrue("Check for sleep message", testResults.contains("Sleeping before executing workflow for 1000 ms."));
        assertTrue("Check for workflow complete", testResults.contains("Docker execution result: \"iteration: 0\"\n" + "\"iteration: 1\"\n"
                + "\"iteration: 2\"\n" + "\"iteration: 3\"\n" + "\"iteration: 4\"\n"));

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output", testResults.contains(begining));

        assertTrue("check INI", testResults.contains("INI is: param1=value1\n" + "param2=value2\n"
                + "param3=help I'm trapped in an INI file"));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        String initalHeartbeat = new String(Files.readAllBytes(Paths.get("src/test/resources/testInitialHeartbeat.txt")));
        assertTrue("Check for an initial heart beat, found" + testResults, testResults.contains(initalHeartbeat));

        // String workflowOutput = new String(Files.readAllBytes(Paths.get("src/test/resources/testFinalHeartbeat.txt")));
        // assertTrue("Check for workflow output", testResults.contains(workflowOutput));

        assertTrue("check for stderr in heartbeat", testResults.contains("\"stderr\": \"123_err\","));
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
}
