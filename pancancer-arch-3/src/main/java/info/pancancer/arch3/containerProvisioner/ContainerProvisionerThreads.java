package info.pancancer.arch3.containerProvisioner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.simple.JSONObject;

/**
 * Created by boconnor on 15-04-18.
 */
public class ContainerProvisionerThreads extends Base {

    private static final int DEFAULT_THREADS = 3;

    public static void main(String[] argv) throws Exception {
        ContainerProvisionerThreads containerProvisionerThreads = new ContainerProvisionerThreads(argv);
        containerProvisionerThreads.startThreads();
    }

    private ContainerProvisionerThreads(String[] argv) throws IOException {
        super();
        super.parseOptions(argv);
    }

    public void startThreads() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(DEFAULT_THREADS);
        ProcessVMOrders processVMOrders = new ProcessVMOrders(this.configFile, this.options.has(this.endlessSpec));
        ProvisionVMs provisionVMs = new ProvisionVMs(this.configFile, this.options.has(this.endlessSpec));
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
                vmChannel.basicConsume(queueName + "_vms", true, consumer);

                // TODO: need threads that each read from orders and another that reads results
                do {
                    System.out.println("CHECKING FOR NEW VM ORDER!");
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received New VM Request '" + message + "'");

                    // now parse it as a VM order
                    Provision p = new Provision();
                    p.fromJSON(message);
                    p.setState(ProvisionState.PENDING);

                    // puts it into the DB so I can count it in another thread
                    db.createProvision(p);
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

        private long maxWorkers = 0;
        private final String configFile;
        private final boolean endless;

        public ProvisionVMs(String configFile, boolean endless) {
            this.configFile = configFile;
            this.endless = endless;
        }

        @Override
        public Void call() throws IOException {
            try {

                JSONObject settings = Utilities.parseConfig(configFile);

                // max number of workers
                maxWorkers = (Long) settings.get("max_running_containers");

                // queueName = (String) settings.get("rabbitMQQueueName");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    // System.out.println("CHECKING RUNNING VMs");

                    // read from DB
                    long numberRunningContainers = db.getProvisionCount(ProvisionState.RUNNING);
                    long numberPendingContainers = db.getProvisionCount(ProvisionState.PENDING);

                    // System.out.println("  CHECKING NUMBER OF RUNNING: "+numberRunningContainers);

                    // if this is true need to launch another container
                    if (numberRunningContainers < maxWorkers && numberPendingContainers > 0) {

                        System.out.println("  RUNNING CONTAINERS < " + maxWorkers + " SO WILL LAUNCH VM");

                        // TODO: this will obviously get much more complicated when integrated with Youxia launch VM
                        String uuid = db.getPendingProvisionUUID();
                        // this just updates one that's pending
                        db.updatePendingProvision(uuid);
                        // now launch the VM... doing this after the update above to prevent race condition if the worker signals
                        // finished
                        // before it's marked as pending
                        launchVM(uuid);

                    }
                } while (endless);

            } catch (ShutdownSignalException | ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        }

        // TOOD: obviously, this will need to launch something using Youxia in the future
        private void launchVM(String uuid) {
            new Worker(configFile, uuid, 1).run();
            System.out.println("\n\n\nI LAUNCHED A WORKER THREAD FOR VM " + uuid + " AND IT'S RELEASED!!!\n\n");
        }

    }

    /**
     * This keeps an eye on the results queue. It updates the database with finished jobs. Presumably it should also kill VMs.
     */
    private static class CleanupVMs implements Callable<Void> {

        private final String configFile;
        private final boolean endless;

        public CleanupVMs(String configFile, boolean endless) {
            this.configFile = configFile;
            this.endless = endless;
        }

        @Override
        public Void call() throws IOException {
            Channel resultsChannel = null;
            try {

                JSONObject settings = Utilities.parseConfig(configFile);

                String queueName = (String) settings.get("rabbitMQQueueName");

                // read from
                resultsChannel = Utilities.setupMultiQueue(settings, queueName + "_results");
                // this declares a queue exchange where multiple consumers get the same message:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                String resultsQueue = resultsChannel.queueDeclare().getQueue();
                resultsChannel.queueBind(resultsQueue, queueName + "_results", "");
                QueueingConsumer resultsConsumer = new QueueingConsumer(resultsChannel);
                resultsChannel.basicConsume(resultsQueue, true, resultsConsumer);

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    System.out.println("CHECKING FOR VMs TO REAP!");

                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] RECEIVED RESULT MESSAGE - ContainerProvisioner: '" + message + "'");

                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(message);

                    // now update that DB record to be exited
                    // this is acutally finishing the VM and not the work
                    if (status.getState() == StatusState.SUCCESS && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // this is where it reaps, the job status message also contains the UUID for the VM
                        db.finishContainer(status.getVmUuid());
                    } else if ((status.getState() == StatusState.RUNNING || status.getState() == StatusState.FAILED
                            || status.getState() == StatusState.PENDING || status.getState() == StatusState.PROVISIONING)
                            && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // deal with running, failed, pending, provisioning
                        // convert from provision state to statestate
                        ProvisionState provisionState = ProvisionState.valueOf(status.getState().toString());
                        db.updateProvision(status.getVmUuid(), status.getJobUuid(), provisionState);
                    }
                } while (endless);

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
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
