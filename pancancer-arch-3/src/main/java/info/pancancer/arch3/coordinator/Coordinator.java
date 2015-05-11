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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
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

    private JSONObject settings = null;

    // private Channel jobChannel = null;
    // private Channel vmChannel = null;
    // private Channel orderChannel = null;
    // private Connection connection = null;
    // private String queueName = null;
    // private Utilities u = new Utilities();

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        if (options.has("config")) {
            configFile = (String) options.valueOf("config");
        }

        // processes orders and turns them into requests for VMs/Containers (handled by ContainerProvisioner) and Jobs (handled by Worker)
        /** CoordinatorOrders t1 = */
        new CoordinatorOrders(configFile);

        // this cleans up Jobs
        /** CleanupJobs t2 = */
        new CleanupJobs(configFile);

        // this marks jobs as lost, resubmits them, etc
        /** FlagJobs t3 = */
        new FlagJobs(configFile);

    }

    public Coordinator(String configFile) {
        settings = Utilities.parseConfig(configFile);
    }

}

class CoordinatorOrders {

    private JSONObject settings = null;
    private Channel jobChannel = null;
    private Channel vmChannel = null;
    private Channel orderChannel = null;
    // private Connection connection = null;
    private String queueName = null;
    // private Utilities u = new Utilities();

    private Inner inner;

    public CoordinatorOrders(String configFile) {
        inner = new Inner(configFile);
    }

    private class Inner extends Thread {

        private String configFile = null;
        private final Logger log = LoggerFactory.getLogger(getClass());

        Inner(String config) {
            super(config);
            configFile = config;
            start();
        }

        @Override
        public void run() {

            try {

                settings = Utilities.parseConfig(configFile);

                PostgreSQL db = new PostgreSQL(settings);

                queueName = (String) settings.get("rabbitMQQueueName");
                // read from
                orderChannel = Utilities.setupQueue(settings, queueName + "_orders");
                // write to
                jobChannel = Utilities.setupQueue(settings, queueName + "_jobs"); // TODO: actually this one needs to be built on demand
                                                                                  // with full
                // info
                // write to
                vmChannel = Utilities.setupQueue(settings, queueName + "_vms");
                // read from

                QueueingConsumer consumer = new QueueingConsumer(orderChannel);
                orderChannel.basicConsume(queueName + "_orders", false, consumer);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
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
                }

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

                JSONObject settings = Utilities.parseConfig(this.configFile);
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

}

/**
 * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
 */
class CleanupJobs {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    // private Channel vmChannel = null;
    private String queueName = null;
    // private Utilities u = new Utilities();
    private QueueingConsumer resultsConsumer = null;

    private Inner inner;

    private class Inner extends Thread {

        private String configFile = null;

        Inner(String config) {
            super(config);
            configFile = config;
            start();
        }

        public void run() {
            try {

                settings = Utilities.parseConfig(configFile);

                queueName = (String) settings.get("rabbitMQQueueName");

                // read from
                resultsChannel = Utilities.setupMultiQueue(settings, queueName + "_results");
                // this declares a queue exchange where multiple consumers get the same message:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                String resultsQueue = resultsChannel.queueDeclare().getQueue();
                resultsChannel.queueBind(resultsQueue, queueName + "_results", "");
                resultsConsumer = new QueueingConsumer(resultsChannel);
                resultsChannel.basicConsume(resultsQueue, true, resultsConsumer);

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery();
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

                }

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            }
            // log.error(ex.toString());
            // log.error(ex.toString());

        }

    }

    public CleanupJobs(String configFile) {
        inner = new Inner(configFile);
    }
}

/**
 * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
 */
class FlagJobs {

    private JSONObject settings = null;
    // private Channel resultsChannel = null;
    // private Channel vmChannel = null;
    // private String queueName = null;
    private Utilities u = new Utilities();
    // private QueueingConsumer resultsConsumer = null;

    private Inner inner;

    private class Inner extends Thread {

        private String configFile = null;
        private final Logger log = LoggerFactory.getLogger(getClass());

        Inner(String config) {
            super(config);
            configFile = config;
            start();
        }

        @Override
        public void run() {
            try {

                settings = Utilities.parseConfig(configFile);

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

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

                }

            } catch (Exception ex) {
                System.out.println(ex.toString());
                throw new RuntimeException(ex);
            }
        }

    }

    public FlagJobs(String configFile) {
        inner = new Inner(configFile);
    }

}
