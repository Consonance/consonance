package info.pancancer.arch3.worker;

import com.rabbitmq.client.*;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.utils.Utilities;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by boconnor on 15-04-18.
 */
public class Worker extends Thread {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel jobChannel = null;
    private Connection connection = null;
    private String queueName = null;
    private Utilities u = new Utilities();
    private String vmUuid = null;

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }

        // TODO: can't run on the command line anymore!
        //Worker w = new Worker(configFile);
        //w.start();

    }

    public Worker(String configFile, String vmUuid) {

        settings = u.parseConfig(configFile);
        queueName = (String) settings.get("rabbitMQQueueName");
        this.vmUuid = vmUuid;

    }

    public void run () {
        try {

            // the VM UUID
            System.out.println(" WORKER VM UUID: '" + vmUuid + "'");

            // read from
            jobChannel = u.setupQueue(settings, queueName + "_jobs");

            // write to
            resultsChannel = u.setupMultiQueue(settings, queueName+"_results");

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            jobChannel.basicConsume(queueName+"_jobs", true, consumer);

            // TODO: need threads that each read from orders and another that reads results
            boolean cont = true;
            while (cont) {

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting
                cont = false;

                // get the job order
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody());
                System.out.println(" [x] Received Jobs request '" + message + "'");

                Job job = new Job().fromJSON(message);

                // TODO: this will obviously get much more complicated when integrated with Docker
                // launch VM
                Status s = new Status(vmUuid, job.getUuid(), u.RUNNING, u.JOB_MESSAGE_TYPE, "job is starting");
                String result = s.toJSON();
                launchJob(result);

                // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own thread, harvesting STDERR/STDOUT periodically
                try {
                    // pause
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    log.error(ex.toString());
                }

                s = new Status(vmUuid, job.getUuid(), u.SUCCESS, u.JOB_MESSAGE_TYPE, "job is finished");
                result = s.toJSON();
                finishJob(result);

            }

        } catch (IOException ex) {
            System.out.println(ex.toString()); ex.printStackTrace();
        } catch (InterruptedException ex) {
            log.error(ex.toString());
        } catch (ShutdownSignalException ex) {
            log.error(ex.toString());
        } catch (ConsumerCancelledException ex) {
            log.error(ex.toString());
        }
    }

    // TOOD: obviously, this will need to launch something using Youxia in the future
    private void launchJob(String message) {
        try {
            resultsChannel.basicPublish(queueName+"_results", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
            //resultsChannel.basicPublish("results", "", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private void finishJob(String message) {
        try {
            resultsChannel.basicPublish(queueName + "_results", "", null, message.getBytes());
            //resultsChannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
