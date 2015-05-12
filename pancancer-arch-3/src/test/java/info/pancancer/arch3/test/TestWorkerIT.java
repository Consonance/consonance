package info.pancancer.arch3.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;

@PrepareForTest({ QueueingConsumer.class, Utilities.class, Worker.class })
@RunWith(PowerMockRunner.class)
public class TestWorkerIT {

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

    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private PrintStream originalOutStream = new PrintStream(System.out);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Utilities.class);
        System.setOut(new PrintStream(outStream));
    }

    @Test
    public void testRunWorker() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {

        Mockito.doNothing().when(mockConnection).close();

        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);

        Mockito.when(Utilities.setupQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        Mockito.when(Utilities.setupMultiQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

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

        Worker testWorker = new Worker("src/test/resources/workerConfig.json", "vm123456", 1);

        testWorker.run();
        String testResults = this.outStream.toString();
        // String knownResults = new String(Files.readAllBytes(Paths.get("src/test/resources/TestWorkerResult.txt")));
        System.setOut(originalOutStream);
        // System.out.println(testResults);

        // Because we are generating temp files with unique names and numeric sequence, simply asserting that the two strings match will not
        // work.
        // The text containing the temp file name must be cleaned before we can assert that the code worked.
        testResults = testResults.replaceAll("seqware_[0-9]+\\.ini", "seqware_tmpfile.ini");
        testResults = testResults.replaceAll("oozie-[a-z0-9\\-]+", "JOB_ID");
        testResults = testResults.replaceAll("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}", "0000/00/00 00:00:00");
        testResults = testResults.replaceAll("bundle_manager\\d+", "bundle_manager_LONG_NUMERIC_SEQUENCE");
        testResults = testResults.replaceAll("scheduler\\d+out", "schedulerLONG_NUMERIC_SEQUENCEout");
        testResults = testResults.replaceAll("\r", "");
        testResults = testResults.replaceAll("IP address: /[^\"]*", "IP address: 0.0.0.0");
        testResults = testResults.replaceAll("/home/[^ /]*/", "/home/\\$USER/");
        System.out.println(testResults);
        // Check for the docker command.
        assertTrue(
                "Check for docker command",
                testResults
                        .contains("Executing command: [docker run --rm -h master -t -v /var/run/docker.sock:/var/run/docker.sock -v /workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0:/workflow -v /tmp/seqware_tmpfile.ini:/ini -v /datastore:/datastore -v /home/$USER/.ssh/gnos.pem:/home/$USER/.ssh/gnos.pem seqware/seqware_whitestar_pancancer seqware bundle launch --dir /workflow --ini /ini --no-metadata]"));
        assertTrue("Check for sleep message", testResults.contains("Sleeping before executing workflow for 1000 ms."));
        assertTrue("Check for workflow complete", testResults.contains("Workflow run 10 is now completed"));

        String begining = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_Start.txt")));
        assertTrue("check begining of output", testResults.startsWith(begining));

        String ending = new String(Files.readAllBytes(Paths.get("src/test/resources/testResult_End.txt")));
        assertTrue("check ending of output", testResults.contains(ending));

        String initalHeartbeat = new String(Files.readAllBytes(Paths.get("src/test/resources/testInitialHeartbeat.txt")));
        assertTrue("Check for an initial heart beat", testResults.contains(initalHeartbeat));

        String workflowOutput = new String(Files.readAllBytes(Paths.get("src/test/resources/testFinalHeartbeat.txt")));
        assertTrue("Check for workflow output", testResults.contains(workflowOutput));
    }
}
