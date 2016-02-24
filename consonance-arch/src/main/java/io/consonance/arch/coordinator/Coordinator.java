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

package io.consonance.arch.coordinator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import io.consonance.arch.Base;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.beans.Order;
import io.consonance.arch.beans.Status;
import io.consonance.arch.beans.StatusState;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 *
 * This consumes the jobs and prepares messages for the VM and Job Queues.
 *
 * It then monitors the results queue to see when jobs fail or finish.
 *
 * Finally, for failed or finished workflows, it informs the VM about finished VMs that can be terminated.
 *
 * TODO:
 *
 * This needs to have a new thread that periodically checks on the DB table for Jobs to identify jobs that are lost/failed
 *
 * @author boconnor
 * @author dyuen
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

        private Channel vmChannel = null;
        private Channel orderChannel = null;
        private String queueName = null;
        private final boolean endless;
        private String configFile = null;
        private final Logger log = LoggerFactory.getLogger(getClass());
        private Channel jobChannel = null;

        private Set<String> existingJobQueues = new HashSet<>();

        public CoordinatorOrders(String config, boolean endless) throws InterruptedException {
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public Void call() throws Exception {
            try {

                HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(configFile);

                queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
                // read from
                orderChannel = CommonServerTestUtilities.setupQueue(settings, queueName + "_orders");
                // write to

                // create the job exchange
                String exchange = queueName + "_job_exchange";
                jobChannel = CommonServerTestUtilities.setupExchange(settings, exchange, "direct");

                // full
                // info
                // write to
                vmChannel = CommonServerTestUtilities.setupQueue(settings, queueName + "_vms");
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

                    requestVm(order.getProvision().toJSON());
                    publishJob(settings, exchange, order.getJob().toJSON());

                    log.info("acknowledging " + delivery.getEnvelope().toString());
                    orderChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } while (endless);

            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException | NullPointerException ex) {
                log.error(ex.getMessage(), ex);
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

            // TODO: should save information to persistent storage

            try {

                log.info(" + SENDING VM ORDER! " + queueName + "_vms");

                int messages = vmChannel.queueDeclarePassive(queueName + "_vms").getMessageCount();
                log.info("  + VM QUEUE SIZE: " + messages);

                vmChannel.basicPublish("", queueName + "_vms", MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));
                vmChannel.waitForConfirms();

                log.info(" + MESSAGE SENT!\n" + message + "\n");

            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            return null;

        }

        /**
         * This sends a Job message to the job exchange.
         *
         * @param settings consonance config file
         * @param message a particular job to schedule
         * @return
         */
        private String publishJob(HierarchicalINIConfiguration settings, String exchangeName, String message) {

            try {
                log.info(" + sending job order! " + queueName + "_jobs");

                PostgreSQL db = new PostgreSQL(settings);
                Job newJob = new Job().fromJSON(message);
                db.updateJob(newJob.getUuid(), newJob.getVmUuid(), JobState.PENDING);
                final String routingKey = newJob.getFlavour();
                // see if a particular queue type exist yet
                if (!existingJobQueues.contains(routingKey)){
                    existingJobQueues.add(routingKey);
                    final String finalQueueName = CommonServerTestUtilities
                            .setupQueueOnExchange(jobChannel, queueName + "_jobs", newJob.getFlavour());
                    jobChannel.queueBind(finalQueueName, exchangeName, newJob.getFlavour());
                }
                jobChannel.basicPublish(exchangeName, newJob.getFlavour() , MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));

                log.info(" + message sent!\n" + message + "\n");
                return message;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    /**
     * This de-queues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
     *
     * This looks like a duplicate class from ContainerProvisionerThreads.
     */
    private static class CleanupJobs implements Callable<Void> {
        protected static final Logger LOG = LoggerFactory.getLogger(CleanupJobs.class);
        private final boolean endless;
        private String configFile = null;

        public CleanupJobs(String config, boolean endless) throws InterruptedException {
            this.endless = endless;
            this.configFile = config;
        }

        @Override
        public Void call() throws IOException, TimeoutException {
            Channel resultsChannel = null;
            try {

                HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(configFile);
                String queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
                final String resultQueueName = queueName + "_results";

                // read from
                resultsChannel = CommonServerTestUtilities.setupExchange(settings, resultQueueName);
                // this declares a queue exchange where multiple consumers get the same message:
                // https://www.rabbitmq.com/tutorials/tutorial-three-java.html
                String resultsQueue = CommonServerTestUtilities.setupQueueOnExchange(resultsChannel, queueName, "CleanupJobs");
                resultsChannel.queueBind(resultsQueue, resultQueueName, "");
                QueueingConsumer resultsConsumer = new QueueingConsumer(resultsChannel);
                resultsChannel.basicConsume(resultsQueue, false, resultsConsumer);

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
                    LOG.info(" [x] RECEIVED RESULT MESSAGE - Coordinator: '" + message + "'");

                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(message);

                    // now update that DB record to be exited
                    // this is actually finishing the VM and not the work
                    if (status.getState() == StatusState.SUCCESS && CommonServerTestUtilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // this is where it reaps, the job status message also contains the UUID for the VM
                        LOG.info("\n\n\nFINISHING THE JOB!!!!!!!!!!!!!!!\n\n");
                        db.finishJob(status.getJobUuid());
                    } else if ((status.getState() == StatusState.RUNNING || status.getState() == StatusState.FAILED || status.getState() == StatusState.PENDING)
                            && CommonServerTestUtilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // this is where it reaps, the job status message also contains the UUID for the VM
                        // convert from StatusState to JobState
                        JobState valueOf = JobState.valueOf(status.getState().toString());
                        db.updateJob(status.getJobUuid(), status.getVmUuid(), valueOf);
                    }

                    // TODO: deal with other situations here like

                    /*
                     * try { // pause Thread.sleep(5000); } catch (InterruptedException ex) { //log.error(ex.toString()); }
                     */
                    resultsChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
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
            HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(configFile);

            // writes to DB as well
            PostgreSQL db = new PostgreSQL(settings);

            // TODO: need threads that each read from orders and another that reads results
            do {

                // checks the jobs in the database and sees if any have become "lost"
                List<Job> jobs = db.getJobs(JobState.RUNNING);

                // how long before we call something lost?
                // it is tempting to un-lose jobs here, but the problem is that we only have the update timestamp and that is modified when
                // jobs are lost, meaning they instantly flip back
                long secBeforeLost = settings.getLong(Constants.COORDINATOR_SECONDS_BEFORE_LOST);

                for (Job job : jobs) {
                    Timestamp nowTs = new Timestamp(new Date().getTime());
                    Timestamp updateTs = job.getUpdateTimestamp();

                    long diff = nowTs.getTime() - updateTs.getTime();
                    long diffSec = Math.abs(diff / Base.ONE_SECOND_IN_MILLISECONDS);

                    log.info(job.getUuid() + " DIFF SEC: " + diffSec + " MAX: " + secBeforeLost);

                    JobState state = job.getState();
                    // if this is true need to mark the job as lost!
                    if (state == JobState.RUNNING && diffSec > secBeforeLost) {
                        // it must be lost
                        log.error("Running job " + job.getUuid() + " not seen in " + diffSec + " > " + secBeforeLost + " MARKING AS LOST!");
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
