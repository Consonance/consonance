package info.pancancer.arch3.test;

import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;

import static org.mockito.Matchers.*;

@PrepareForTest({ QueueingConsumer.class, Utilities.class, Worker.class })
@RunWith(PowerMockRunner.class)
public class TestWorker {

    @Mock
    Utilities mockUtil;

    @Mock
    Channel mockChannel;

    @Mock
    QueueingConsumer mockConsumer;

    @Mock
    Envelope mockEnvelope;

    @Mock
    BasicProperties mockProperties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunWorker() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException, Exception {

        Mockito.when(mockUtil.setupQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        Mockito.when(mockUtil.setupMultiQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("rabbitMQQueueName", "seqware");
        Mockito.when(mockUtil.parseConfig(anyString())).thenReturn(jsonObj);
        
        Job j = new Job();
        j.setWorkflow("testWorkflow");
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");
        byte[] body = j.toJSON().getBytes();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        //PowerMockito.whenNew(QueueingConsumer.class).withAnyArguments().thenReturn(mockConsumer);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

        PowerMockito.whenNew(Utilities.class).withNoArguments().thenReturn(mockUtil);

        Worker testWorker = new Worker("src/test/resources/workerConfig.json", "vm123456");
        
        //Whitebox.setInternalState(testWorker,"u",mockUtil);
        testWorker.run();
    }
}
