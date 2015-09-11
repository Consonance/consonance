package info.consonance.arch.test;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import info.consonance.arch.beans.Job;
import info.consonance.arch.utils.Utilities;
import info.consonance.arch.worker.WorkerRunnable;
import info.consonance.arch.worker.WorkflowRunner;
import info.consonance.arch.utils.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertTrue;
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
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PrepareForTest({ QueueingConsumer.class, Utilities.class, WorkerRunnable.class, DefaultExecutor.class, WorkflowRunner.class,
        DefaultExecuteResultHandler.class, Logger.class, LoggerFactory.class, HierarchicalINIConfiguration.class })
@RunWith(PowerMockRunner.class)
public class TestWorkerWithMocking {

    @Mock
    private HierarchicalINIConfiguration config;

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

    @Mock
    private Appender<LoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> argCaptor;

    private static ch.qos.logback.classic.Logger LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Before
    public void setUp() throws IOException, TimeoutException {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Utilities.class);

        Mockito.when(mockAppender.getName()).thenReturn("MOCK");
        LOG.addAppender((Appender) mockAppender);

        Mockito.doNothing().when(mockConnection).close();
        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);
        Mockito.when(Utilities.setupQueue(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);
        Mockito.when(Utilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);
    }

    private String appendEventsIntoString(List<LoggingEvent> events) {
        StringBuffer sbuff = new StringBuffer();
        for (LoggingEvent e : events) {
            sbuff.append("\n" + e.getMessage());
        }
        return sbuff.toString();
    }

    @Test
    public void testRunWorker() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {

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
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

        WorkerRunnable testWorker = new WorkerRunnable("src/test/resources/workerConfig.ini", "vm123456", 1);

        testWorker.run();
        // String testResults = TestWorkerWithMocking.outBuffer.toString();// this.outStream.toString();

        Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(argCaptor.capture());
        List<LoggingEvent> tmpList = new LinkedList<LoggingEvent>(argCaptor.getAllValues());
        String testResults = this.appendEventsIntoString(tmpList);

        testResults = cleanResults(testResults);
        // System.out.println("\n===============================\nTest Results: " + testResults);
        // System.out.println(testResults);
        String expectedDockerCommand = "docker run --rm -h master -t -v /var/run/docker.sock:/var/run/docker.sock -v /workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0:/workflow -v /tmp/seqware_tmpfile.ini:/ini -v /datastore:/datastore -v /home/$USER/.gnos:/home/$USER/.gnos -v /home/$USER/custom-seqware-settings:/home/seqware/.seqware/settings pancancer/seqware_whitestar_pancancer:1.1.1 seqware bundle launch --dir /workflow --ini /ini --no-metadata --engine whitestar";
        // System.out.println(expectedDockerCommand);
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
        Mockito.when(config.getString(Constants.RABBIT_QUEUE_NAME)).thenReturn("seqware");
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
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(config);
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
