package info.pancancer.arch3.containerProvisioner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import info.pancancer.arch3.worker.Worker;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;

import java.io.IOException;

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
        ContainerProvisionerThreads c = new ContainerProvisionerThreads(configFile);

        // the thread that handles reading the queue and writing to the DB
        ProcessVMOrders t1 = new ProcessVMOrders(configFile);

        // this actually launches worker daemons
        ProvisionVMs t2 = new ProvisionVMs(configFile);

        // CleanupVMs t3 = new CleanupVMs(configFile);
    }

    private ContainerProvisionerThreads(String configFile) {

        settings = u.parseConfig(configFile);

    }


}


/**
 * This examines the provision table in the DB to identify the number of running VMs. It then
 * figures out if the number running is < the max number.  If so it picks the oldest pending,
 * switches to running, and launches a worker thread.
 */
class ProvisionVMs {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel vmChannel = null;
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

        public void run() {
            try {

                settings = u.parseConfig(configFile);

                // max number of workers
                maxWorkers = (Long) settings.get("max_running_containers");

                queueName = (String) settings.get("rabbitMQQueueName");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                // write to
                //resultsChannel = u.setupMultiQueue(settings, queueName + "_results");

                //QueueingConsumer consumer = new QueueingConsumer(vmChannel);
                //vmChannel.basicConsume(queueName + "_vms", true, consumer);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

                    // read from DB
                    int numberRunningContainers = db.getProvisionCount(Utilities.PENDING);

                    // if this is true need to launch another container
                    if (numberRunningContainers < maxWorkers) {
                        // TODO: this will obviously get much more complicated when integrated with Youxia launch VM
                        String result = "{ \"VM-launched-message\": {} }";
                        launchVM(result);
                        // this just updates one that's pending
                        db.updatePendingProvision();
                    }

                    try {
                        // pause
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        //log.error(ex.toString());
                    }

                }

            } catch (ShutdownSignalException ex) {
                //log.error(ex.toString());
            } catch (ConsumerCancelledException ex) {
                //log.error(ex.toString());
            }
        }

        // TOOD: obviously, this will need to launch something using Youxia in the future
        private void launchVM(String message) {

            new Worker(configFile).start();

        }

    }

    public ProvisionVMs(String configFile) {
        inner = new Inner(configFile);
    }
}



/**
 * This dequeues the VM requests and stages them in the DB as pending so I can
 * keep a count of what's running/pending/finished.
 */
class ProcessVMOrders {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel vmChannel = null;
    private String queueName = null;
    private Utilities u = new Utilities();

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

                settings = u.parseConfig(configFile);

                queueName = (String) settings.get("rabbitMQQueueName");

                // read from
                vmChannel = u.setupQueue(settings, queueName + "_vms");

                // write to
                resultsChannel = u.setupMultiQueue(settings, queueName + "_results");

                // writes to DB as well
                PostgreSQL db = new PostgreSQL(settings);

                QueueingConsumer consumer = new QueueingConsumer(vmChannel);
                vmChannel.basicConsume(queueName + "_vms", true, consumer);

                // TODO: need threads that each read from orders and another that reads results
                while (true) {

                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = new String(delivery.getBody());
                    System.out.println(" [x] Received VM request '" + message + "'");

                    // now parse it as a VM order
                    Provision p = new Provision();
                    p.fromJSON(message);
                    p.setState(Utilities.PENDING);
                    db.createProvision(p);

                    // TODO: this will obviously get much more complicated when integrated with Youxia launch VM
                    String result = "{ \"VM-launched-message\": {} }";
                    //launchVM(result);

                    try {
                        // pause
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        //log.error(ex.toString());
                    }

                }

            } catch (IOException ex) {
                System.out.println(ex.toString());
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                //log.error(ex.toString());
            } catch (ShutdownSignalException ex) {
                //log.error(ex.toString());
            } catch (ConsumerCancelledException ex) {
                //log.error(ex.toString());
            }
        }

    }


    // TOOD: obviously, this will need to launch something using Youxia in the future
    private void launchVM(String message) {
        //try {
            //resultsChannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
            //resultsChannel.basicPublish(queueName + "_results", "", null, message.getBytes());

            /*
            As a temporary test this code will launch a worker thread which will run for one job then
            exit.  This will allow us to simulate
             */

            // TODO: launch thread for worke

        /*} catch (IOException e) {
            //log.error(e.toString());
        }*/
    }

    public ProcessVMOrders(String configFile) {
        inner = new Inner(configFile);
    }
}




