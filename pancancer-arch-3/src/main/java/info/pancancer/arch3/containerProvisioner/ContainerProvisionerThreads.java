package info.pancancer.arch3.containerProvisioner;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.WorkerRunnable;
import io.cloudbindle.youxia.deployer.Deployer;
import io.cloudbindle.youxia.reaper.Reaper;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import joptsimple.OptionSpecBuilder;
import org.apache.commons.exec.CommandLine;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by boconnor on 15-04-18.
 */
public class ContainerProvisionerThreads extends Base {

    private static final int DEFAULT_THREADS = 3;
    private static final int MINUTE_IN_MILLISECONDS = 60 * 1000;
    private final OptionSpecBuilder testSpec;
    public static final String MAX_RUNNING_CONTAINERS = "max_running_containers";

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
     * This dequeues the VM requests from the VM queue and stages them in the DB as pending so I can keep a count of what's
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
        public Void call() throws IOException {
            Channel vmChannel = null;
            try {

                JSONObject settings = Utilities.parseConfig(config);

                String queueName = (String) settings.get("rabbitMQQueueName");

                // read from
                vmChannel = Utilities.setupQueue(settings, queueName + "_vms");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                QueueingConsumer consumer = new QueueingConsumer(vmChannel);
                vmChannel.basicConsume(queueName + "_vms", false, consumer);

                // TODO: need threads that each read from orders and another that reads results
                do {
                    LOG.info("CHECKING FOR NEW VM ORDER!");
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
                    db.createProvision(p);
                    vmChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
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

                JSONObject settings = Utilities.parseConfig(configFile);
                if (!settings.containsKey(MAX_RUNNING_CONTAINERS)) {
                    LOG.info("No max_running_containers specified, skipping provision ");
                    return null;
                }

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    // System.out.println("CHECKING RUNNING VMs");

                    // read from DB
                    long numberRunningContainers = db.getJobs(JobState.PENDING).size();
                    long numberPendingContainers = db.getJobs(JobState.RUNNING).size();

                    if (testMode) {
                        LOG.debug("  CHECKING NUMBER OF RUNNING: " + numberRunningContainers);
                        maxWorkers = (Long) settings.get(MAX_RUNNING_CONTAINERS);

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
                        if (requiredVMs > 0) {
                            String param = (String) settings.get("youxia_deployer_parameters");
                            CommandLine parse = CommandLine.parse("dummy " + (param == null ? "" : param));
                            List<String> arguments = new ArrayList<>();
                            arguments.addAll(Arrays.asList(parse.getArguments()));
                            arguments.add("--total-nodes-num");
                            arguments.add(String.valueOf(requiredVMs));
                            String[] toArray = arguments.toArray(new String[arguments.size()]);
                            LOG.info("Running youxia deployer with following parameters:" + Arrays.toString(toArray));
                            // need to make sure reaper and deployer do not overlap
                            synchronized (ContainerProvisionerThreads.class) {
                                try {
                                    Deployer.main(toArray);
                                } catch (Exception e) {
                                    LOG.error("Youxia deployer threw the following exception", e);
                                }
                            }
                            if (endless) {
                                Thread.sleep(MINUTE_IN_MILLISECONDS);
                            }
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

                JSONObject settings = Utilities.parseConfig(configFile);

                String queueName = (String) settings.get("rabbitMQQueueName");
                final String resultQueueName = queueName + "_results";

                // read from
                resultsChannel = Utilities.setupExchange(settings, resultQueueName);
                // this declares a queue exchange where multiple consumers get the same message:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                String resultsQueue = Utilities.setupQueueOnExchange(resultsChannel, queueName, "CleanupVMs");
                resultsChannel.queueBind(resultsQueue, resultQueueName, "");
                QueueingConsumer resultsConsumer = new QueueingConsumer(resultsChannel);
                resultsChannel.basicConsume(resultsQueue, false, resultsConsumer);

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    LOG.info("CHECKING FOR VMs TO REAP!");

                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    LOG.info(" [x] RECEIVED RESULT MESSAGE - ContainerProvisioner: '" + message + "'");

                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(message);

                    // in end states, keep a copy of the results
                    if (status.getState() == StatusState.SUCCESS || status.getState() == StatusState.FAILED) {
                        db.updateJobMessage(status.getJobUuid(), status.getStdout(), status.getStderr());
                    }

                    // now update that DB record to be exited
                    // this is acutally finishing the VM and not the work
                    if (status.getState() == StatusState.SUCCESS && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        if (!settings.containsKey(MAX_RUNNING_CONTAINERS)) {
                            LOG.info("No max_running_containers specified, skipping provision ");
                            continue;
                        }

                        // this is where it reaps, the job status message also contains the UUID for the VM
                        db.finishContainer(status.getVmUuid());
                        String param = (String) settings.get("youxia_reaper_parameters");
                        CommandLine parse = CommandLine.parse("dummy " + (param == null ? "" : param));
                        List<String> arguments = new ArrayList<>();
                        arguments.addAll(Arrays.asList(parse.getArguments()));
                        arguments.add("--kill-list");
                        // create a json file with the one targetted ip address
                        Gson gson = new Gson();
                        String[] successfulVMAddresses = db.getSuccessfulVMAddresses();
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
                        synchronized (ContainerProvisionerThreads.class) {
                            try {
                                Reaper.main(toArray);
                            } catch (Exception e) {
                                LOG.error("Youxia reaper threw the following exception", e);
                            }
                        }
                        if (endless) {
                            Thread.sleep(MINUTE_IN_MILLISECONDS);
                        }
                    } else if ((status.getState() == StatusState.RUNNING || status.getState() == StatusState.FAILED
                            || status.getState() == StatusState.PENDING || status.getState() == StatusState.PROVISIONING)
                            && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // deal with running, failed, pending, provisioning
                        // convert from provision state to statestate
                        ProvisionState provisionState = ProvisionState.valueOf(status.getState().toString());
                        db.updateProvisionByJobUUID(status.getJobUuid(), status.getVmUuid(), provisionState, status.getIpAddress());
                    }
                    resultsChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException ex) {
                LOG.error("CleanupVMs threw the following exception", ex);
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
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
}
