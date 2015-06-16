/*
 * Copyright (C) 2015 CancerCollaboratory
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.pancancer.arch3.reporting;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

/**
 * The Arch3ReportImpl implements calls that are specific to arch3 for retrieving reporting. 
 * This means that this means that this class will need to be totally replaced if we swap out the queuing system
 * @author dyuen
 */
public class Arch3ReportImpl implements ReportAPI {

    public static final int LOOP_LIMIT = 1000;

    private final HierarchicalINIConfiguration settings;
    private final PostgreSQL db;

    public Arch3ReportImpl(HierarchicalINIConfiguration config, PostgreSQL postgres) {
        this.settings = config;
        this.db = postgres;
    }

    @Override
    public Map<ProvisionState, Long> getVMStateCounts() {
        Map<ProvisionState, Long> map = new HashMap<>();
        for (ProvisionState state : ProvisionState.values()) {
            long provisionCount = db.getProvisionCount(state);
            if (provisionCount > 0) {
                map.put(state, provisionCount);
            }
        }
        return map;
    }

    @Override
    public Map<JobState, Integer> getJobStateCounts() {
        Map<JobState, Integer> map = new HashMap<>();
        for (JobState state : JobState.values()) {
            List<Job> jobs = db.getJobs(state);
            if (jobs.size() > 0) {
                map.put(state, jobs.size());
            }
        }
        return map;
    }

    @Override
    public Map<String, Status> getLastStatus() {
        String queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
        final String resultQueueName = queueName + "_results";
        String resultsQueue = null;

        Channel resultsChannel = null;
        synchronized (Arch3ReportImpl.this) {
            try {
                // read from
                resultsChannel = Utilities.setupExchange(settings, resultQueueName);
                // this declares a queue exchange where multiple consumers get the same convertToResult:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                resultsQueue = Utilities.setupQueueOnExchange(resultsChannel, queueName, "SlackReportBot");
                resultsChannel.queueBind(resultsQueue, resultQueueName, "");
                QueueingConsumer resultsConsumer = new QueueingConsumer(resultsChannel);
                resultsChannel.basicConsume(resultsQueue, false, resultsConsumer);

                int messagesToCache = db.getJobs(JobState.RUNNING).size();
                Map<String, Status> cache = new HashMap<>();

                int loop = 0;
                do {
                    loop++;
                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery(Base.FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    String messageFromQueue = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(messageFromQueue);
                    cache.put(status.getIpAddress(), status);
                } while (loop < LOOP_LIMIT && cache.size() < messagesToCache);

                return cache;

            } catch (IOException | ShutdownSignalException | InterruptedException | ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            } finally {
                try {
                    if (resultsQueue != null && resultsChannel != null) {
                        resultsChannel.queueDelete(resultsQueue);
                    }
                    if (resultsChannel != null) {

                        resultsChannel.close();
                        resultsChannel.getConnection().close();
                    }
                } catch (IOException ex) {
                    System.err.println("Could not close channel");
                }
            }
        }
    }

    @Override
    public Map<String, String> getEnvironmentMap() {
        Map<String, String> env = new TreeMap<>();
        env.put("version", this.getClass().getPackage().getImplementationVersion());
        try {
            Constants constantsInstance = new Constants();
            Class constants = Class.forName("info.pancancer.arch3.utils.Constants");
            Field[] declaredFields = constants.getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.getName().contains("PASSWORD")) {
                    continue;
                }
                String settingKey = field.get(constantsInstance).toString();
                if (this.settings.containsKey(settingKey)) {
                    env.put(settingKey, this.settings.getString(settingKey));
                }
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        return env;
    }

    @Override
    public Map<String, String> getCommands() {
        Map<String, String> map = new TreeMap<>();
        map.put("status", "retrieves configuration and version information on arch3");
        map.put("info", "retrieves detailed information on provisioned instances");
        map.put("provisioned", "retrieves detailed information on provisioned instances");
        map.put("jobs", "retrieves detailed information on jobs");
        map.put("gather", "gathers the last message sent by each worker and displays the last line of it");
        return map;
    }
}
