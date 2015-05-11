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
import java.util.Date;
import java.util.List;
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

    public static void main(String[] argv) throws Exception {
        Coordinator coordinator = new Coordinator(argv);
        coordinator.doWork();
    }

    public Coordinator(String[] argv) throws IOException {
        super();
        parseOptions(argv);
    }

    public void doWork() throws InterruptedException {
        CoordinatorOrders coordinatorOrders = new CoordinatorOrders(this.configFile, this.options.has(this.endlessSpec));
        CleanupJobs cleanupJobs = new CleanupJobs(this.configFile, this.options.has(this.endlessSpec));
        FlagJobs flagJobs = new FlagJobs(this.configFile, this.options.has(this.endlessSpec));
        coordinatorOrders.start();
        cleanupJobs.start();
        flagJobs.start();
        coordinatorOrders.join();
        cleanupJobs.join();
        flagJobs.join();
    }

    private static class CoordinatorOrders extends Thread {

        private JSONObject settings = null;
        private Channel jobChannel = null;
        private Channel vmChannel = null;
        private Channel orderChannel = null;
        private String queueName = null;
        private final Utilities u = new Utilities();
        private final boolean endless;
        private String configFile = null;
        private final Logger log = LoggerFactory.getLogger(getClass());

        public CoordinatorOrders(String config, boolean endless) throws InterruptedException {
            super("CoordinatorOrders");
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public void run() {

            try {

                settings = u.parseConfig(configFile);

                PostgreSQL db = new PostgreSQL(settings);

                queueName = (String) settings.get("rabbitMQQueueName");
                // read from
                orderChannel = u.setupQueue(settings, queueName + "_orders");
                // write to
                jobChannel = u.setupQueue(settings, queueName + "_jobs"); // TODO: actually this one needs to be built on demand with
                                                                          // full
                                                                          // info
                // write to
                vmChannel = u.setupQueue(settings, queueName + "_vms");
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
                    System.out.println(" [x] RECEIVED ORDER:\n'" + message + "'\n");

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

                        System.out.println("\n\nSKIPPING JOB WITH HASH " + order.getJob().getJobHash()
                                + " PREVIOUSLY SUBMITTED/FAILED/RUNNING/SUCCESSFUL\n");

                    }

                    System.out.println("acknowledging " + delivery.getEnvelope().toString());
                    orderChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException ex) {
                System.out.println(ex.toString());
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
                log.error(ex.toString());
            }
        }

        private String requestVm(String message) {

            // TODO: should save information to persistant storage

            try {

                System.out.println(" + SENDING VM ORDER! " + queueName + "_vms");

                int messages = vmChannel.queueDeclarePassive(queueName + "_vms").getMessageCount();
                System.out.println("  + VM QUEUE SIZE: " + messages);

                vmChannel.basicPublish("", queueName + "_vms", MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));

                System.out.println(" + MESSAGE SENT!\n" + message + "\n");

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            return null;

        }

        private String requestJob(String message) {

            StringBuilder result = new StringBuilder();

            try {

                // TODO: future feature...
                // So this is strange, why does the queue name have all this info in it? It's
                // because we may have orders for the same workflow that actually need different resources
                // Channel vmchannel = u.setupQueue(settings,
                // queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb);

                System.out.println(" + SENDING JOB ORDER! " + queueName + "_jobs");

                int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                System.out.println("  + JOB QUEUE SIZE: " + messages);

                jobChannel.basicPublish("", queueName + "_jobs", MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));

                JSONObject settings = u.parseConfig(this.configFile);
                PostgreSQL db = new PostgreSQL(settings);
                Job newJob = new Job().fromJSON(message);
                newJob.setState(JobState.PENDING);
                db.createJob(newJob);

                System.out.println(" + MESSAGE SENT!\n" + message + "\n");

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return result.toString();
        }

    }

    /**
     * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
     */
    private static class CleanupJobs extends Thread {

        private JSONObject settings = null;
        private Channel resultsChannel = null;
        private String queueName = null;
        private final Utilities u = new Utilities();
        private QueueingConsumer resultsConsumer = null;
        private final boolean endless;
        private String configFile = null;

        public CleanupJobs(String config, boolean endless) throws InterruptedException {
            super("Cleanup Jobs");
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public void run() {
            try {

                settings = u.parseConfig(configFile);

                queueName = (String) settings.get("rabbitMQQueueName");

                // read from
                resultsChannel = u.setupMultiQueue(settings, queueName + "_results");
                // this declares a queue exchange where multiple consumers get the same message:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                String resultsQueue = resultsChannel.queueDeclare().getQueue();
                resultsChannel.queueBind(resultsQueue, queueName + "_results", "");
                resultsConsumer = new QueueingConsumer(resultsChannel);
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
            }
            // log.error(ex.toString());
            // log.error(ex.toString());

        }
    }

    /**
     * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
     */
    private static class FlagJobs extends Thread {

        private JSONObject settings = null;
        private final Utilities u = new Utilities();
        private final boolean endless;
        private final String configFile;
        private final Logger log = LoggerFactory.getLogger(getClass());

        public FlagJobs(String config, boolean endless) {
            super("FlagJobs");
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public void run() {
            settings = u.parseConfig(configFile);

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
        }
    }

}
