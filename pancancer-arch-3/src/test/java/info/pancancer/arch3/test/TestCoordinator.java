package info.pancancer.arch3.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.coordinator.Coordinator;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk;

@PrepareForTest({ QueueingConsumer.class, Utilities.class, Coordinator.class, Logger.class, LoggerFactory.class, PostgreSQL.class })
@RunWith(PowerMockRunner.class)
public class TestCoordinator {
    private static final Logger log = LoggerFactory.getLogger(TestCoordinator.class);
    @Mock
    private Channel mockChannel;

    @Mock
    private DeclareOk mockDeclareOk;

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

    @Mock
    private Connection mockDBConnection;
    
    @Mock
    private QueryRunner mockRunner;
    
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
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        outBuffer = new StringBuffer();
        Mockito.doAnswer(logAnswer).when(mockLogger).info(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).debug(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).error(anyString());
        Mockito.doAnswer(logAnswer).when(mockLogger).error(anyString(), any(Exception.class));
        PowerMockito.mockStatic(org.slf4j.LoggerFactory.class);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(Base.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(Coordinator.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(PostgreSQL.class)).thenReturn(mockLogger);
        Mockito.when(org.slf4j.LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);
        PowerMockito.mockStatic(Utilities.class);

        Mockito.doNothing().when(mockConnection).close();

        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);

        Mockito.when(Utilities.setupQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

        Mockito.when(Utilities.setupMultiQueue(any(JSONObject.class), anyString())).thenReturn(mockChannel);

    }

    @Test
    public void testCoordinator_badDBConfig() throws InterruptedException, Exception {
        setupConfig(false);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        try {
            Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/config.json" });
            testCoordinator.doWork();
            fail("Should not have reached here.");
        } catch (Exception e) {
            assertTrue(outBuffer
                    .toString()
                    .contains(
                            "The following configuration values are null: postgresHost postgresUser postgresPass postgresDBName . Please check your configuration file."));
        }
    }

    @Test
    public void testCoordinator_invalidDB() throws InterruptedException, Exception {
        setupConfig(true);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        try {
            Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/coordinatorConfig.json" });
            testCoordinator.doWork();
            fail("Should not have reached here.");
        } catch (Exception e) {
            //System.out.println(outBuffer.toString());
            assertTrue(outBuffer.toString().contains("invalid database address: jdbc:postgresql://localhost/dbname"));
        }
    }

    @Test
    public void testCoordinator() throws InterruptedException, Exception {
        setupMockDB();
        setupConfig(true);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

//        try {
            Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/coordinatorConfig.json" });
            testCoordinator.doWork();
            System.out.println(outBuffer.toString());
//            fail("Should not have reached here.");
//        } catch (Exception e) {
//            //
//            assertTrue(outBuffer.toString().contains("invalid database address: jdbc:postgresql://localhost/dbname"));
//        }
    }
    
    private void setupMockDB() throws Exception
    {
        PowerMockito.mockStatic(DriverManager.class);
        Mockito.when(DriverManager.getConnection(anyString(), any(Properties.class))).thenReturn(mockDBConnection);
       
        Mockito.when(mockRunner.query(any(Connection.class), anyString(), any(ResultSetHandler.class), anyVararg())).thenReturn(new HashMap());
        PowerMockito.whenNew(QueryRunner.class).withNoArguments().thenReturn(mockRunner);
    }
    
    private byte[] setupMessage() {
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
        return body;
    }

    private void setupConfig(boolean withPostgresConfig) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("rabbitMQQueueName", "seqware");
        jsonObj.put("heartbeatRate", "2.5");
        jsonObj.put("preworkerSleep", "1");
        jsonObj.put("postworkerSleep", "1");
        jsonObj.put("hostUserName", System.getProperty("user.name"));
        jsonObj.put("max_seconds_before_lost", new Long("1"));
        if (withPostgresConfig) {
            jsonObj.put("postgresHost", "localhost");
            jsonObj.put("postgresUser", "user");
            jsonObj.put("postgresPass", "password");
            jsonObj.put("postgresDBName", "dbname");
        }
        Mockito.when(Utilities.parseConfig(anyString())).thenReturn(jsonObj);
    }

    private void setupMockQueue(Delivery testDelivery) throws InterruptedException, Exception {
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        Mockito.when(mockDeclareOk.getQueue()).thenReturn("mockQueue");
        PowerMockito.whenNew(DeclareOk.class).withAnyArguments().thenReturn(mockDeclareOk);

        Mockito.when(mockChannel.queueDeclare()).thenReturn(mockDeclareOk);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

    }
}
