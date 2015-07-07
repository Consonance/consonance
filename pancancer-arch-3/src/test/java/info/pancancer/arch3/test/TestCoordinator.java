package info.pancancer.arch3.test;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.coordinator.Coordinator;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Connection mockDBConnection;

    @Mock
    private QueryRunner mockRunner;

    @Mock
    private PoolingDataSource<PoolableConnection> poolingDataSource;

    private static StringBuffer outBuffer = new StringBuffer();

    @Before
    public void setup() throws IOException, IOException, TimeoutException {
        MockitoAnnotations.initMocks(this);

        outBuffer = new StringBuffer();

        PowerMockito.mockStatic(Utilities.class);

        Mockito.doNothing().when(mockConnection).close();

        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);

        Mockito.when(Utilities.setupQueue(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

        Mockito.when(Utilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

    }

    @Test(expected = Exception.class)
    public void testCoordinator_badDBConfig() throws InterruptedException, Exception {
        setupConfig(false);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/config" });
        testCoordinator.doWork();
        fail("Should not have reached here.");

    }

    @Test(expected = Exception.class)
    public void testCoordinator_invalidDB() throws InterruptedException, Exception {
        setupConfig(true);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/coordinatorConfig" });
        testCoordinator.doWork();
        fail("Should not have reached here.");

    }

    // @Test
    // public void testCoordinator() throws InterruptedException, Exception {
    // setupMockDB();
    // setupConfig(true);
    // byte[] body = setupMessage();
    // Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
    // setupMockQueue(testDelivery);
    //
    // // try {
    // File file = FileUtils.getFile("src", "test", "resources", "config.json");
    // Coordinator testCoordinator = new Coordinator(new String[] { "--config", file.getAbsolutePath() });
    // testCoordinator.doWork();
    // System.out.println(outBuffer.toString());
    // // fail("Should not have reached here.");
    // // } catch (Exception e) {
    // // //
    // // assertTrue(outBuffer.toString().contains("invalid database address: jdbc:postgresql://localhost/dbname"));
    // // }
    // }

    private void setupMockDB() throws Exception {
        PowerMockito.mockStatic(DriverManager.class);
        Mockito.when(DriverManager.getConnection(anyString(), any(Properties.class))).thenReturn(mockDBConnection);

        Mockito.when(mockRunner.query(any(Connection.class), anyString(), any(ResultSetHandler.class), anyVararg())).thenReturn(
                new HashMap());
        PowerMockito.whenNew(QueryRunner.class).withNoArguments().thenReturn(mockRunner);
        PowerMockito.whenNew(PoolingDataSource.class).withAnyArguments().thenReturn(poolingDataSource);
        Mockito.when(poolingDataSource.getConnection()).thenReturn(mockDBConnection);
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

    private void setupConfig(boolean withPostgresConfig) {
        HierarchicalINIConfiguration jsonObj = new HierarchicalINIConfiguration();
        jsonObj.addProperty(Constants.RABBIT_QUEUE_NAME, "seqware");
        jsonObj.addProperty(Constants.WORKER_HEARTBEAT_RATE, "2.5");
        jsonObj.addProperty(Constants.WORKER_PREWORKER_SLEEP, "1");
        jsonObj.addProperty(Constants.WORKER_POSTWORKER_SLEEP, "1");
        jsonObj.addProperty(Constants.WORKER_HOST_USER_NAME, System.getProperty("user.name"));
        jsonObj.addProperty(Constants.COORDINATOR_SECONDS_BEFORE_LOST, 1L);
        if (withPostgresConfig) {
            jsonObj.addProperty(Constants.POSTGRES_HOST, "localhost");
            jsonObj.addProperty(Constants.POSTGRES_USERNAME, "user");
            jsonObj.addProperty(Constants.POSTGRES_PASSWORD, "password");
            jsonObj.addProperty(Constants.POSTGRES_DBNAME, "dbname");
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
