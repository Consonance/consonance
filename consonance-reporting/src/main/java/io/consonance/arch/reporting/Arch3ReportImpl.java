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
package io.consonance.arch.reporting;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.consonance.arch.Base;
import info.consonance.arch.CloudTypes;
import info.consonance.arch.beans.Job;
import info.consonance.arch.beans.JobState;
import info.consonance.arch.beans.Provision;
import info.consonance.arch.beans.ProvisionState;
import info.consonance.arch.beans.Status;
import io.consonance.arch.persistence.PostgreSQL;
import info.consonance.arch.utils.Constants;
import info.consonance.arch.utils.Utilities;
import io.cloudbindle.youxia.listing.AbstractInstanceListing;
import io.cloudbindle.youxia.listing.ListingFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

import io.consonance.arch.persistence.PostgreSQL;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

/**
 * The Arch3ReportImpl implements calls that are specific to arch3 for retrieving reporting. This means that this means that this class will
 * need to be totally replaced if we swap out the queuing system
 *
 * @author dyuen
 */
public class Arch3ReportImpl implements ReportAPI {

    public static final int LOOP_LIMIT = 1000;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final double MILLISECONDS_IN_SECOND = 1000.0;

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
                Map<String, Status> cache = new TreeMap<>();

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

            } catch (IOException | ShutdownSignalException | InterruptedException | TimeoutException | ConsumerCancelledException ex) {
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
                } catch (IOException | TimeoutException ex) {
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
            Class constants = Class.forName("Constants");
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

        try {
            // stolen from stack overflow
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            String ip;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream(), StandardCharsets.UTF_8))) {
                ip = in.readLine(); // you get the IP as a String
            }
            // reverse ip address
            InetAddress addr = InetAddress.getByName(ip);
            env.put("canonical_host_name", addr.getCanonicalHostName());
            env.put("host_name", addr.getHostName());
        } catch (IOException e) {
            System.out.println("Could not lookup ip address of self");
        }

        return env;
    }

    @Override
    public Map<String, String> getCommands() {
        Map<String, String> map = new TreeMap<>();
        for (Commands command : Commands.values()) {
            map.put(command.toString(), command.getDescription());
        }
        return map;
    }

    @Override
    public Map<String, Map<String, String>> getJobInfo() {
        Map<String, Map<String, String>> map = new TreeMap<>();
        for (JobState state : JobState.values()) {
            List<Job> jobs = db.getJobs(state);
            Date now = new Date();
            Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());
            long time = currentTimestamp.getTime();
            DecimalFormat df = new DecimalFormat("#.00");
            for (Job job : jobs) {
                Map<String, String> jobMap = new TreeMap<>();
                jobMap.put("status", job.getState().toString());
                jobMap.put("workflow", job.getWorkflow());
                jobMap.put("workflow_version", job.getWorkflowVersion());
                long lastSeen = job.getUpdateTs().getTime();
                double secondsAgo = (time - lastSeen) / MILLISECONDS_IN_SECOND;
                jobMap.put("last seen (seconds)", df.format(secondsAgo));
                long firstSeen = job.getCreateTs().getTime();
                double hoursAgo = (time - firstSeen) / (MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR);
                jobMap.put("first seen(hours)", df.format(hoursAgo));
                map.put(job.getUuid(), jobMap);
            }

        }
        return map;
    }

    @Override
    public Map<String, Map<String, String>> getVMInfo() {
        return this.getVMInfo(ProvisionState.values());
    }

    @Override
    public Map<String, Map<String, String>> getVMInfo(ProvisionState... states) {
        Map<String, AbstractInstanceListing.InstanceDescriptor> youxiaInstances = this.getYouxiaInstances();
        Set<String> activeIPAddresses = new HashSet<>();
        // curate set of ip addresses for active instances
        for (Entry<String, AbstractInstanceListing.InstanceDescriptor> entry : youxiaInstances.entrySet()) {
            activeIPAddresses.add(entry.getValue().getIpAddress());
            activeIPAddresses.add(entry.getValue().getPrivateIpAddress());
        }
        System.out.println("Set of active addresses: " + activeIPAddresses.toString());

        // this map is provision_uuid -> keys -> values
        Map<String, Map<String, String>> map = new TreeMap<>();
        for (ProvisionState state : states) {
            List<Provision> provisions = db.getProvisions(state);
            Date now = new Date();
            Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());
            long time = currentTimestamp.getTime();
            DecimalFormat df = new DecimalFormat("#.00");
            for (Provision provision : provisions) {
                System.out.println("Checking " + provision.getIpAddress());
                if (!activeIPAddresses.contains(provision.getIpAddress())) {
                    System.out.println("Excluding " + provision.getIpAddress() + " due to youxia filter");
                    continue;
                }
                Map<String, String> jobMap = new TreeMap<>();
                jobMap.put("ip_address", provision.getIpAddress());
                jobMap.put("status", provision.getState().toString());
                long lastSeen = provision.getUpdateTimestamp().getTime();
                double secondsAgo = (time - lastSeen) / MILLISECONDS_IN_SECOND;
                jobMap.put("last seen with live heartbeat (seconds)", df.format(secondsAgo));
                long firstSeen = provision.getCreateTimestamp().getTime();
                double hoursAgo = (time - firstSeen) / (MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR);
                jobMap.put("first seen in submission queue (hours)", df.format(hoursAgo));
                map.put(provision.getProvisionUUID(), jobMap);
            }
        }
        return map;
    }

    @Override
    public Map<String, AbstractInstanceListing.InstanceDescriptor> getYouxiaInstances() {
        Map<String, AbstractInstanceListing.InstanceDescriptor> youxiaInstances = new HashMap<>();
        for (CloudTypes type : CloudTypes.values()) {
            youxiaInstances.putAll(this.getYouxiaInstances(type));
        }
        return youxiaInstances;
    }

    @Override
    public Map<String, AbstractInstanceListing.InstanceDescriptor> getYouxiaInstances(CloudTypes cloudType) {
        try {
            AbstractInstanceListing listing;
            if (cloudType == CloudTypes.AWS) {
                listing = ListingFactory.createAWSListing();

            } else if (cloudType == CloudTypes.AZURE) {
                listing = ListingFactory.createAzureListing();

            } else {
                listing = ListingFactory.createOpenStackListing();
            }
            return listing.getInstances();
        } catch (Exception e) {
            // return a blank listing is there is nothing
            return new HashMap<>();
        }
    }
}
