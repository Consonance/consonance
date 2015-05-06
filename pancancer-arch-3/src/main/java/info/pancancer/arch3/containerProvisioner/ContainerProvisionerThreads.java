package info.pancancer.arch3.containerProvisioner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;

/**
 * Created by boconnor on 15-04-18.
 */
public class ContainerProvisionerThreads extends Base {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel vmChannel = null;
    private String queueName = null;
    private Utilities u = new Utilities();

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        if (options.has("config")) {
            configFile = (String) options.valueOf("config");
        }
        // this isn't really used...
        /** ContainerProvisionerThreads c = */
        new ContainerProvisionerThreads(configFile);

        // the thread that handles reading the queue and writing to the DB
        /** ProcessVMOrders t1 = */
        new ProcessVMOrders(configFile);

        // this actually launches worker daemons
        /** ProvisionVMs t2 = */
        new ProvisionVMs(configFile);

        // this cleans up VMs, currently this is just a DB update but in the future it's a Youxia call
        /** CleanupVMs t3 = */
        new CleanupVMs(configFile);
    }

    private ContainerProvisionerThreads(String configFile) {

        settings = u.parseConfig(configFile);

    }

}

/**
 * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
 */
class ProcessVMOrders {

    private JSONObject settings = null;
    private Channel vmChannel = null;
    private String queueName = null;
    private final Utilities u = new Utilities();

    private final Inner inner;

    private class Inner extends Thread {

        private String configFile = null;

        Inner(String config) {
            super(config);
            configFile = config;
            start();
        }

        @Override
        public void run() {
            try {

                settings = u.parseConfig(configFile);

                queueName = (String) settings.get("rabbitMQQueueName");

                // read from
                vmChannel = u.setupQueue(settings, queueName + "_vms");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                QueueingConsumer consumer = new QueueingConsumer(vmChannel);
                vmChannel.basicConsume(queueName + "_vms", true, consumer);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

                    System.out.println("CHECKING FOR NEW VM ORDER!");

                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received New VM Request '" + message + "'");

                    // now parse it as a VM order
                    Provision p = new Provision();
                    p.fromJSON(message);
                    p.setState(Utilities.PENDING);

                    // puts it into the DB so I can count it in another thread
                    db.createProvision(p);

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

    public ProcessVMOrders(String configFile) {
        inner = new Inner(configFile);
    }
}

/**
 * This examines the provision table in the DB to identify the number of running VMs. It then figures out if the number running is < the max
 * number. If so it picks the oldest pending, switches to running, and launches a worker thread.
 */
class ProvisionVMs {

    private JSONObject settings = null;
    private final Channel resultsChannel = null;
    private final Channel vmChannel = null;
    private String queueName = null;
    private Utilities u = new Utilities();
    private long maxWorkers = 0;

    private Inner inner;

    private class Inner extends Thread {

        private String configFile = null;

        Inner(String config) {
            super(config);
            configFile = config;
            start();
        }

        @Override
        public void run() {
            try {

                settings = u.parseConfig(configFile);

                // max number of workers
                maxWorkers = (Long) settings.get("max_running_containers");

                queueName = (String) settings.get("rabbitMQQueueName");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

                    // System.out.println("CHECKING RUNNING VMs");

                    // read from DB
                    int numberRunningContainers = db.getProvisionCount(Utilities.RUNNING);
                    int numberPendingContainers = db.getProvisionCount(Utilities.PENDING);

                    // System.out.println("  CHECKING NUMBER OF RUNNING: "+numberRunningContainers);

                    // if this is true need to launch another container
                    if (numberRunningContainers < maxWorkers && numberPendingContainers > 0) {

                        System.out.println("  RUNNING CONTAINERS < " + maxWorkers + " SO WILL LAUNCH VM");

                        // TODO: this will obviously get much more complicated when integrated with Youxia launch VM
                        String uuid = db.getPendingProvisionUUID();
                        // this just updates one that's pending
                        db.updatePendingProvision(uuid);
                        // now launch the VM... doing this after the update above to prevent race condition if the worker signals finished
                        // before it's marked as pending
                        launchVM(uuid);

                    }

                    /*
                     * try { // pause Thread.sleep(5000); } catch (InterruptedException ex) { //log.error(ex.toString()); }
                     */

                }

            } catch (ShutdownSignalException ex) {
                throw new RuntimeException(ex);
            } catch (ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            }
        }

        // TOOD: obviously, this will need to launch something using Youxia in the future
        private void launchVM(String uuid) {

            new Worker(configFile, uuid, 1).start();

            System.out.println("\n\n\nI LAUNCHED A WORKER THREAD FOR VM " + uuid + " AND IT'S RELEASED!!!\n\n");

        }

    }

    public ProvisionVMs(String configFile) {
        inner = new Inner(configFile);
    }
}

/**
 * This dequeues the VM requests and stages them in the DB as pending so I can keep a count of what's running/pending/finished.
 */
class CleanupVMs {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private final Channel vmChannel = null;
    private String queueName = null;
    private Utilities u = new Utilities();
    private QueueingConsumer resultsConsumer = null;

    private Inner inner;

    private class Inner extends Thread {

        private String configFile = null;

        Inner(String config) {
            super(config);
            configFile = config;
            start();
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
                while (true) {

                    System.out.println("CHECKING FOR VMs TO REAP!");

                    QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery();
                    // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] RECEIVED RESULT MESSAGE - ContainerProvisioner: '" + message + "'");

                    // now parse it as JSONObj
                    Status status = new Status().fromJSON(message);

                    // now update that DB record to be exited
                    // this is acutally finishing the VM and not the work
                    if (status.getState().equals(Utilities.SUCCESS) && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // this is where it reaps, the job status message also contains the UUID for the VM
                        db.finishContainer(status.getVmUuid());
                    } else if ((status.getState().equals(Utilities.RUNNING) || status.getState().equals(Utilities.FAILED)
                            || status.getState().equals(Utilities.PENDING) || status.getState().equals(Utilities.PROVISIONING))
                            && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
                        // deal with running, failed, pending, provisioning
                        db.updateProvision(status.getVmUuid(), status.getJobUuid(), status.getState());
                    }

                    /*
                     * try { // pause Thread.sleep(5000); } catch (InterruptedException ex) { System.err.println(ex.toString()); }
                     */

                }

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    public CleanupVMs(String configFile) {
        inner = new Inner(configFile);
    }
}
