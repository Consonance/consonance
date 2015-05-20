package info.pancancer.arch3.coordinator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by boconnor on 15-04-18.
 *
 * This consumes the jobs and prepares messages for the VM and Job Queues.
 *
 * It then monitors the results queue to see when jobs fail or finish.
 *
 * Finally, for failed or finished workflows, it informats the VM about finished VMs that can be terminated.
 *
 * TODO:
 *
 * This needs to have a new thread that periodically checks on the DB table for Jobs to identify jobs that are lost/failed
 *
 *
 */
public class Coordinator extends Base {

    private static final int DEFAULT_THREADS = 3;

    public static void main(String[] argv) throws Exception {
        Coordinator coordinator = new Coordinator(argv);
        coordinator.doWork();
    }

    public Coordinator(String[] argv) throws IOException {
        super();
        parseOptions(argv);
    }

    public void doWork() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(DEFAULT_THREADS);
        CoordinatorOrders coordinatorOrders = new CoordinatorOrders(this.configFile, this.options.has(this.endlessSpec));
        CleanupJobs cleanupJobs = new CleanupJobs(this.configFile, this.options.has(this.endlessSpec));
        FlagJobs flagJobs = new FlagJobs(this.configFile, this.options.has(this.endlessSpec));
        List<Future<?>> futures = new ArrayList<>();
        futures.add(pool.submit(coordinatorOrders));
        futures.add(pool.submit(cleanupJobs));
        futures.add(pool.submit(flagJobs));
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
     * Reads from the Order queue and breaks it up into VMs for the VM queue and jobs for the job queue.
     */
    private static class CoordinatorOrders implements Callable<Void> {

        private Channel jobChannel = null;
        private Channel vmChannel = null;
        private Channel orderChannel = null;
        private String queueName = null;
        private final boolean endless;
        private String configFile = null;
        private final Logger log = LoggerFactory.getLogger(getClass());

        public CoordinatorOrders(String config, boolean endless) throws InterruptedException {
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public Void call() throws Exception {
            try {

                JSONObject settings = Utilities.parseConfig(configFile);

                PostgreSQL db = new PostgreSQL(settings);

                queueName = (String) settings.get("rabbitMQQueueName");
                // read from
                orderChannel = Utilities.setupQueue(settings, queueName + "_orders");
                // write to
                jobChannel = Utilities.setupQueue(settings, queueName + "_jobs"); // TODO: actually this one needs to be built on demand
                                                                                  // with
                // full
                // info
                // write to
                vmChannel = Utilities.setupQueue(settings, queueName + "_vms");
                // read from

                QueueingConsumer consumer = new QueueingConsumer(orderChannel);
                orderChannel.basicConsume(queueName + "_orders", false, consumer);

                // TODO: need threads that each read from orders and another that reads results
                do {

                    QueueingConsumer.Delivery delivery = consumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    log.info(" [x] RECEIVED ORDER:\n'" + message + "'\n");

                    // run the job
                    Order order = new Order().fromJSON(message);

                    boolean checkPreviousRuns = true;
                    String checkSetting = (String) settings.get("check_previous_job_hash");
                    if ("false".equalsIgnoreCase(checkSetting)) {
                        checkPreviousRuns = false;
                    }

                    if ((checkPreviousRuns && !db.previouslyRun(order.getJob().getJobHash())) || !checkPreviousRuns) {

                        requestVm(order.getProvision().toJSON());
                        requestJob(order.getJob().toJSON());

                    } else {

                        log.info("\n\nSKIPPING JOB WITH HASH " + order.getJob().getJobHash()
                                + " PREVIOUSLY SUBMITTED/FAILED/RUNNING/SUCCESSFUL\n");

                    }

                    log.info("acknowledging " + delivery.getEnvelope().toString());
                    orderChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException ex) {
                log.error(ex.getMessage(),ex);
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException | NullPointerException ex) {
                log.error(ex.getMessage(),ex);
            } finally {
                // orderChannel.close();
                if (orderChannel != null) {
                    orderChannel.getConnection().close();
                }
                // jobChannel.close();
                if (jobChannel != null) {
                    jobChannel.getConnection().close();
                }
                // vmChannel.close();
                if (vmChannel != null) {
                    vmChannel.getConnection().close();
                }
            }
            return null;
        }

        /**
         * Requests a new VM from the VM queue.
         *
         * @param message
         *            a JSON representation of a Provision
         * @return
         */
        private String requestVm(String message) {

            // TODO: should save information to persistant storage

            try {

                log.info(" + SENDING VM ORDER! " + queueName + "_vms");

                int messages = vmChannel.queueDeclarePassive(queueName + "_vms").getMessageCount();
                log.info("  + VM QUEUE SIZE: " + messages);

                vmChannel.basicPublish("", queueName + "_vms", MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));

                log.info(" + MESSAGE SENT!\n" + message + "\n");

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            return null;

        }

        /**
         * This sends a Job message to the job queue.
         *
         * @param message
         * @return
         */
        private String requestJob(String message) {

            StringBuilder result = new StringBuilder();

            try {

                // TODO: future feature...
                // So this is strange, why does the queue name have all this info in it? It's
                // because we may have orders for the same workflow that actually need different resources
                // Channel vmchannel = u.setupQueue(settings,
                // queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb);

               log.info(" + SENDING JOB ORDER! " + queueName + "_jobs");

                int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                log.info("  + JOB QUEUE SIZE: " + messages);

                jobChannel.basicPublish("", queueName + "_jobs", MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));

                JSONObject settings = Utilities.parseConfig(this.configFile);
                PostgreSQL db = new PostgreSQL(settings);
                Job newJob = new Job().fromJSON(message);
                newJob.setState(JobState.PENDING);
                db.createJob(newJob);

                log.info(" + MESSAGE SENT!\n" + message + "\n");

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return result.toString();
        }

    }

    /**
     * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
     *
     * This looks like a duplicate class from ContainerProvisionerThreads.
     */
    private static class CleanupJobs implements Callable<Void> {

        private final boolean endless;
        private String configFile = null;

        public CleanupJobs(String config, boolean endless) throws InterruptedException {
            this.endless = endless;
            this.configFile = config;
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

                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery(FIVE_SECOND_IN_MILLISECONDS);
                    if (delivery == null) {
                        continue;
                    }
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] RECEIVED RESULT MESSAGE - Coordinator: '" + message + "'");

                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(message);

                    // now update that DB record to be exited
                    // this is acutally finishing the VM and not the work
                    if (status.getState() == StatusState.SUCCESS && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // this is where it reaps, the job status message also contains the UUID for the VM
                        System.out.println("\n\n\nFINISHING THE JOB!!!!!!!!!!!!!!!\n\n");
                        db.finishJob(status.getJobUuid());
                    } else if ((status.getState() == StatusState.RUNNING || status.getState() == StatusState.FAILED || status.getState() == StatusState.PENDING)
                            && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // this is where it reaps, the job status message also contains the UUID for the VM
                        // convert from StatusState to JobState
                        JobState valueOf = JobState.valueOf(status.getState().toString());
                        db.updateJob(status.getJobUuid(), status.getVmUuid(), valueOf);
                    }

                    // TODO: deal with other situations here like

                    /*
                     * try { // pause Thread.sleep(5000); } catch (InterruptedException ex) { //log.error(ex.toString()); }
                     */

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
            // log.error(ex.toString());
            // log.error(ex.toString());
            return null;
        }

    }

    /**
     * This looks for jobs in the database that have not been updated in a while to determine if they are lost.
     */
    private static class FlagJobs implements Callable<Void> {

        private final boolean endless;
        private final String configFile;
        private final Logger log = LoggerFactory.getLogger(getClass());

        public FlagJobs(String config, boolean endless) {
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public Void call() {
            JSONObject settings = Utilities.parseConfig(configFile);

            // writes to DB as well
            PostgreSQL db = new PostgreSQL(settings);

            // TODO: need threads that each read from orders and another that reads results
            do {

                // checks the jobs in the database and sees if any have become "lost"
                List<Job> jobs = db.getJobs(JobState.RUNNING);

                // how long before we call something lost?
                long secBeforeLost = (Long) settings.get("max_seconds_before_lost");

                for (Job job : jobs) {
                    Timestamp nowTs = new Timestamp(new Date().getTime());
                    Timestamp updateTs = job.getUpdateTs();

                    long diff = nowTs.getTime() - updateTs.getTime();
                    long diffSec = diff / Base.ONE_SECOND_IN_MILLISECONDS;

                    log.error("DIFF SEC: " + diffSec + " MAX: " + secBeforeLost);

                    // if this is true need to mark the job as lost!
                    if (diffSec > secBeforeLost) {
                        // it must be lost
                        log.error("JOB " + job.getUuid() + " NOT SEEN IN " + diffSec + " > " + secBeforeLost + " MARKING AS LOST!");
                        db.updateJob(job.getUuid(), job.getVmUuid(), JobState.LOST);
                    }

                }

                try {
                    // pause
                    Thread.sleep(Base.FIVE_SECOND_IN_MILLISECONDS);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

            } while (endless);
            return null;
        }

    }

    // public FlagJobs(String configFile) {
    // inner = new Inner(configFile);
    // }

}
