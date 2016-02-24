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

package io.consonance.arch.containerProvisioner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import io.cloudbindle.youxia.deployer.Deployer;
import io.cloudbindle.youxia.reaper.Reaper;
import io.consonance.arch.Base;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.beans.ProvisionState;
import io.consonance.arch.beans.Status;
import io.consonance.arch.beans.StatusState;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import io.consonance.arch.worker.WorkerRunnable;
import joptsimple.OptionSpecBuilder;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * @author boconnor
 * @author dyuen
 */
public class ContainerProvisionerThreads extends Base {

    private static final int DEFAULT_THREADS = 3;
    private static final int TWO_MINUTE_IN_MILLISECONDS = 2 * 60 * 1000;

    private final OptionSpecBuilder testSpec;
    protected static final Logger LOG = LoggerFactory.getLogger(ContainerProvisionerThreads.class);

    public static void main(String[] argv) throws Exception {
        ContainerProvisionerThreads containerProvisionerThreads = new ContainerProvisionerThreads(argv);
        containerProvisionerThreads.startThreads();
    }

    private ContainerProvisionerThreads(String[] argv) throws IOException {
        super();
        this.testSpec = super.parser.accepts("test", "in test mode, workers are created directly");
        super.parseOptions(argv);
    }

    public void startThreads() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(DEFAULT_THREADS);
        ProcessVMOrders processVMOrders = new ProcessVMOrders(this.configFile, this.options.has(this.endlessSpec));
        ProvisionVMs provisionVMs = new ProvisionVMs(this.configFile, this.options.has(this.endlessSpec), this.options.has(testSpec));
        CleanupVMs cleanupVMs = new CleanupVMs(this.configFile, this.options.has(this.endlessSpec));
        List<Future<?>> futures = new ArrayList<>();
        futures.add(pool.submit(processVMOrders));
        futures.add(pool.submit(provisionVMs));
        futures.add(pool.submit(cleanupVMs));
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            log.error(ex.toString());
            throw new RuntimeException(ex);
        } finally {
            pool.shutdown();
        }
    }

    /**
     * This de-queues the VM requests from the VM queue and stages them in the DB as pending so I can keep a count of what's
     * running/pending/finished.
     */
    private static class ProcessVMOrders implements Callable<Void> {

        protected static final Logger LOG = LoggerFactory.getLogger(ProcessVMOrders.class);
        private final boolean endless;
        private final String config;

        public ProcessVMOrders(String config, boolean endless) {
            this.endless = endless;
            this.config = config;
        }

        @Override
        public Void call() throws IOException, TimeoutException {
            Channel vmChannel = null;
            try {

                HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(config);

                String queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);

                // read from
                vmChannel = CommonServerTestUtilities.setupQueue(settings, queueName + "_vms");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                QueueingConsumer consumer = new QueueingConsumer(vmChannel);
                vmChannel.basicConsume(queueName + "_vms", false, consumer);

                // TODO: need threads that each read from orders and another that reads results
                do {
                    LOG.debug("CHECKING FOR NEW VM ORDER!");
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    LOG.info(" [x] Received New VM Request '" + message + "'");

                    // now parse it as a VM order
                    Provision p = new Provision();
                    p.fromJSON(message);
                    p.setState(ProvisionState.PENDING);

                    // puts it into the DB so I can count it in another thread
                    db.updateProvisionByJobUUID(p.getJobUUID(),p.getProvisionUUID(),p.getState(),p.getIpAddress());
                    vmChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException | InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            } finally {
                if (vmChannel != null) {
                    vmChannel.close();
                    vmChannel.getConnection().close();
                }
            }
            return null;
        }
    }

    /**
     * This examines the provision table in the DB to identify the number of running VMs. It then figures out if the number running is less
     * than the max number. If so it picks the oldest pending, switches to running, and launches a worker thread.
     */
    private static class ProvisionVMs implements Callable<Void> {
        protected static final Logger LOG = LoggerFactory.getLogger(ProvisionVMs.class);
        private long maxWorkers = 0;
        private final String configFile;
        private final boolean endless;
        private final boolean testMode;

        public ProvisionVMs(String configFile, boolean endless, boolean testMode) {
            this.configFile = configFile;
            this.endless = endless;
            this.testMode = testMode;
        }

        @Override
        public Void call() throws Exception {
            try {

                HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(configFile);
                if (!settings.containsKey(Constants.PROVISION_MAX_RUNNING_CONTAINERS)) {
                    LOG.info("No max_running_containers specified, skipping provision ");
                    return null;
                }

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    LOG.info("Checking running VMs");
                    // System.out.println("CHECKING RUNNING VMs");

                    // read from DB
                    final List<Job> pendingJobs = db.getJobs(JobState.PENDING);
                    long numberRunningContainers = pendingJobs.size();
                    final List<Job> runningJobs = db.getJobs(JobState.RUNNING);
                    long numberPendingContainers = runningJobs.size();
                    LOG.info("Found " + numberRunningContainers + " pending containers and " + numberPendingContainers + " running containers.");

                    if (testMode) {
                        LOG.debug("  CHECKING NUMBER OF RUNNING: " + numberRunningContainers);
                        maxWorkers = settings.getLong(Constants.PROVISION_MAX_RUNNING_CONTAINERS);

                        // if this is true need to launch another container
                        if (numberRunningContainers < maxWorkers && numberPendingContainers > 0) {

                            LOG.info("  RUNNING CONTAINERS < " + maxWorkers + " SO WILL LAUNCH VM");

                            // TODO: this will obviously get much more complicated when integrated with Youxia launch VM
                            // fake a uuid
                            String uuid = UUID.randomUUID().toString().toLowerCase();
                            // now launch the VM... doing this after the update above to prevent race condition if the worker signals
                            // finished
                            // before it's marked as pending
                            new WorkerRunnable(configFile, uuid, 1).run();
                            LOG.info("\n\n\nI LAUNCHED A WORKER THREAD FOR VM " + uuid + " AND IT'S RELEASED!!!\n\n");
                        }
                    } else {
                        long requiredVMs = numberRunningContainers + numberPendingContainers;
                        // determine mix of VMs required
                        Map<String, Integer> clientTypes = new HashMap<>();
                        for(Job j : pendingJobs){
                            clientTypes.compute(j.getFlavour(), (k, v) -> (v == null ? 1 : v + 1));
                        }
                        for(Job j : runningJobs){
                            clientTypes.compute(j.getFlavour(), (k,v) -> (v == null? 1 : v+1));
                        }

                        // cap the number of VMs
                        LOG.info("  Desire for " + clientTypes + " VMs");
                        requiredVMs = Math
                                .min(requiredVMs, settings.getLong(Constants.PROVISION_MAX_RUNNING_CONTAINERS, Integer.MAX_VALUE));
                        // cap the types of VMs
                        final long localRequiredVMs = requiredVMs;
                        while(clientTypes.size() > requiredVMs){
                            clientTypes.replaceAll((k,v) -> clientTypes.size() > localRequiredVMs? (v-1) : v);
                        }
                        LOG.info("  Capped at " + clientTypes + " VMs");
                        if (requiredVMs > 0) {
                            // serialize clientTypes
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            final String required = gson.toJson(clientTypes);
                            final File tempFile = Files.createTempFile("neededVMs", "json").toFile();
                            FileUtils.write(tempFile, required);

                            String param = settings.getString(Constants.PROVISION_YOUXIA_DEPLOYER);
                            CommandLine parse = CommandLine.parse("dummy " + (param == null ? "" : param));
                            List<String> arguments = new ArrayList<>();
                            arguments.addAll(Arrays.asList(parse.getArguments()));
                            arguments.add("--instance-types");
                            arguments.add(tempFile.getAbsolutePath());
                            String[] toArray = arguments.toArray(new String[arguments.size()]);
                            LOG.info("Running youxia deployer with following parameters:" + Arrays.toString(toArray));
                            // need to make sure reaper and deployer do not overlap
                            synchronized (ContainerProvisionerThreads.class) {
                                try {
                                    Deployer.main(toArray);
                                } catch (Exception e) {
                                    LOG.error("Youxia deployer threw the following exception", e);
                                    // call the reaper to do cleanup when deployment fails
                                    runReaper(settings, null, null);
                                }
                            }
                        }
                        if (endless) {
                            // lengthen time to allow cleanup queue to purge
                            Thread.sleep(TWO_MINUTE_IN_MILLISECONDS);
                        }
                    }
                } while (endless);

            } catch (ShutdownSignalException | ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        }

    }

    /**
     * This keeps an eye on the results queue. It updates the database with finished jobs. Presumably it should also kill VMs.
     */
    private static class CleanupVMs implements Callable<Void> {
        protected static final Logger LOG = LoggerFactory.getLogger(CleanupVMs.class);
        private final String configFile;
        private final boolean endless;

        public CleanupVMs(String configFile, boolean endless) {
            this.configFile = configFile;
            this.endless = endless;
        }

        @Override
        public Void call() throws Exception {
            Channel resultsChannel = null;
            try {

                HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(configFile);

                String queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
                final String exchangeName = queueName + "_results";

                // read from
                resultsChannel = CommonServerTestUtilities.setupExchange(settings, exchangeName);
                // this declares a queue exchange where multiple consumers get the same message:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                String resultsQueue = CommonServerTestUtilities.setupQueueOnExchange(resultsChannel, queueName, "CleanupVMs");
                resultsChannel.queueBind(resultsQueue, exchangeName, "");
                QueueingConsumer resultsConsumer = new QueueingConsumer(resultsChannel);
                resultsChannel.basicConsume(resultsQueue, false, resultsConsumer);

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                boolean reapFailedWorkers = settings.getBoolean(Constants.PROVISION_REAP_FAILED_WORKERS, false);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    LOG.debug("CHECKING FOR VMs TO REAP!");

                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    LOG.info(" [x] RECEIVED RESULT MESSAGE - ContainerProvisioner: '" + message + "'");

                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(message);

                    // in end states, keep a copy of the results
                    if (status.getState() == StatusState.SUCCESS || status.getState() == StatusState.FAILED) {
                        db.updateJobMessage(status.getJobUuid(), status.getStdout(), status.getStderr());
                    }

                    if (CommonServerTestUtilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // now update that DB record to be exited
                        // this is actually finishing the VM and not the work
                        if (status.getState() == StatusState.SUCCESS) {
                            // finishing the container means a success status
                            // this is where it reaps, the job status message also contains the UUID for the VM
                            db.finishContainer(status.getVmUuid());
                            synchronized (ContainerProvisionerThreads.class) {
                                runReaper(settings, status.getIpAddress(), status.getVmUuid());
                            }
                        } else if (reapFailedWorkers && status.getState() == StatusState.FAILED) {
                            // reaped failed workers need to be set to the failed state
                            ProvisionState provisionState = ProvisionState.FAILED;
                            db.updateProvisionByJobUUID(status.getJobUuid(), status.getVmUuid(), provisionState, status.getIpAddress());
                            synchronized (ContainerProvisionerThreads.class) {
                                runReaper(settings, status.getIpAddress(), status.getVmUuid());
                            }
                        } else if (status.getState() == StatusState.RUNNING || status.getState() == StatusState.FAILED
                                || status.getState() == StatusState.PENDING || status.getState() == StatusState.PROVISIONING) {
                            // deal with running, failed, pending, provisioning
                            // convert from provision state to statestate
                            ProvisionState provisionState = ProvisionState.valueOf(status.getState().toString());
                            db.updateProvisionByJobUUID(status.getJobUuid(), status.getVmUuid(), provisionState, status.getIpAddress());
                        }
                    }
                    resultsChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException | InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
                LOG.error("CleanupVMs threw the following exception", ex);
                throw new RuntimeException(ex);
            } catch (Exception ex) {
                LOG.error("CleanupVMs threw the following exception", ex);
                throw new RuntimeException(ex);
            } finally {
                if (resultsChannel != null) {
                    resultsChannel.close();
                    resultsChannel.getConnection().close();
                }
            }
            return null;
        }
    }

    /**
     * run the reaper
     *
     * @param settings represents a standard properties file
     * @param ipAddress
     *            specify an ip address (otherwise cleanup only failed deployments)
     * @throws IOException
     */
    private static void runReaper(HierarchicalINIConfiguration settings, String ipAddress, String vmName) throws IOException {
        String param = settings.getString(Constants.PROVISION_YOUXIA_REAPER);
        CommandLine parse = CommandLine.parse("dummy " + (param == null ? "" : param));
        List<String> arguments = new ArrayList<>();
        arguments.addAll(Arrays.asList(parse.getArguments()));

        arguments.add("--kill-list");
        // create a json file with the one targeted ip address
        Gson gson = new Gson();
        // we can't use the full set of database records because unlike Amazon, OpenStack reuses private ip addresses (very quickly too)
        // String[] successfulVMAddresses = db.getSuccessfulVMAddresses();
        String[] successfulVMAddresses = new String[] {};
        if (ipAddress != null) {
            successfulVMAddresses = new String[] { ipAddress, vmName };
        }
        LOG.info("Kill list contains: " + Arrays.asList(successfulVMAddresses));
        Path createTempFile = Files.createTempFile("target", "json");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(createTempFile.toFile()),
                StandardCharsets.UTF_8))) {
            gson.toJson(successfulVMAddresses, bw);
        }
        arguments.add(createTempFile.toAbsolutePath().toString());

        String[] toArray = arguments.toArray(new String[arguments.size()]);
        LOG.info("Running youxia reaper with following parameters:" + Arrays.toString(toArray));
        // need to make sure reaper and deployer do not overlap

        try {
            Reaper.main(toArray);
        } catch (Exception e) {
            LOG.error("Youxia reaper threw the following exception", e);
        }
    }
}
