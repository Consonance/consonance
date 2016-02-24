/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.consonance.arch.test;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk;
import io.consonance.arch.beans.Job;
import io.consonance.arch.coordinator.Coordinator;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@PrepareForTest({ QueueingConsumer.class, CommonServerTestUtilities.class, Coordinator.class, Logger.class, LoggerFactory.class, PostgreSQL.class })
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
    public void setup() throws IOException, TimeoutException, InterruptedException {
        MockitoAnnotations.initMocks(this);

        outBuffer = new StringBuffer();

        PowerMockito.mockStatic(CommonServerTestUtilities.class);

        Mockito.doNothing().when(mockConnection).close();

        Mockito.when(mockChannel.getConnection()).thenReturn(mockConnection);

        Mockito.when(CommonServerTestUtilities.setupQueue(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

        Mockito.when(CommonServerTestUtilities.setupExchange(any(HierarchicalINIConfiguration.class), anyString())).thenReturn(mockChannel);

    }

    @Test(expected = Exception.class)
    public void testCoordinator_badDBConfig() throws Exception {
        setupConfig(false);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/config" });
        testCoordinator.doWork();
        fail("Should not have reached here.");

    }

    @Test(expected = Exception.class)
    public void testCoordinator_invalidDB() throws Exception {
        setupConfig(true);
        byte[] body = setupMessage();
        Delivery testDelivery = new Delivery(mockEnvelope, mockProperties, body);
        setupMockQueue(testDelivery);

        Coordinator testCoordinator = new Coordinator(new String[] { "--config", "src/test/resources/coordinatorConfig" });
        testCoordinator.doWork();
        fail("Should not have reached here.");

    }

    private byte[] setupMessage() {
        Job j = new Job();
        j.setJobHash("asdlk2390aso12jvrej");
        j.setUuid("1234567890");
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
        Mockito.when(CommonTestUtilities.parseConfig(anyString())).thenReturn(jsonObj);
    }

    private void setupMockQueue(Delivery testDelivery) throws Exception {
        Mockito.when(mockConsumer.nextDelivery()).thenReturn(testDelivery);

        Mockito.when(mockDeclareOk.getQueue()).thenReturn("mockQueue");
        PowerMockito.whenNew(DeclareOk.class).withAnyArguments().thenReturn(mockDeclareOk);

        Mockito.when(mockChannel.queueDeclare()).thenReturn(mockDeclareOk);
        PowerMockito.whenNew(QueueingConsumer.class).withArguments(mockChannel).thenReturn(mockConsumer);

    }
}
