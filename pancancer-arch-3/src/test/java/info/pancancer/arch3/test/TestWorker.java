package info.pancancer.arch3.test;

import static org.junit.Assert.assertEquals;
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
import org.junit.Ignore;
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
public class TestWorker {

    @Mock
    Utilities mockUtil;

    @Mock
    Channel mockChannel;

    @Mock
    com.rabbitmq.client.Connection mockConnection;    
    
    @Mock
    QueueingConsumer mockConsumer;

    @Mock
    Envelope mockEnvelope;

    @Mock
    BasicProperties mockProperties;

    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private PrintStream originalOutStream = new PrintStream(System.out);
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        System.setOut(new PrintStream(outStream));
    }

    @Ignore
    @Test
    public void testRunWorker() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {

        Mockito.doNothing().when(mockConnection).close();

        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);
        
        Mockito.when(mockUtil.setupQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        Mockito.when(mockUtil.setupMultiQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("rabbitMQQueueName", "seqware");
        Mockito.when(mockUtil.parseConfig(anyString())).thenReturn(jsonObj);
        
        Job j = new Job();
        j.setWorkflowPath("/workflows/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0");
        j.setWorkflow("HelloWorld");
        j.setWorkflowVersion("1.0-SNAPSHOT");
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");
        Map<String,String> iniMap = new HashMap<String,String>(3);
        iniMap.put("param1", "value1");
        iniMap.put("param2", "value2");
        iniMap.put("param3", "help I'm trapped in an INI file");
        j.setIni(iniMap);
        byte[] body = j.toJSON().getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        //PowerMockito.whenNew(QueueingConsumer.class).withAnyArguments().thenReturn(mockConsumer);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

        PowerMockito.whenNew(Utilities.class).withNoArguments().thenReturn(mockUtil);

        Worker testWorker = new Worker("src/test/resources/workerConfig.json", "vm123456",1);
        
        //Whitebox.setInternalState(testWorker,"u",mockUtil);
        testWorker.run();
        String testResults = this.outStream.toString();
        String knownResults = new String(Files.readAllBytes(Paths.get("src/test/resources/TestWorkerResult.txt")));
        System.setOut(originalOutStream);
        System.out.println(testResults);
       
        //Because we are generating temp files with unique names, simply asserting that the two strings match will not work.
        //The text containing the temp file name must be cleaned before we can assert that the code worked.
        knownResults = knownResults.replaceAll("seqware_[0-9]+\\.ini", "seqware_tmpfile.ini");
        testResults = testResults.replaceAll("seqware_[0-9]+\\.ini", "seqware_tmpfile.ini");
        assertEquals(knownResults,testResults);
    }
}
