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
import java.util.Random;

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
        parser.accepts("uuid").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        String uuid = null;
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }
        if (options.has("uuid")) { uuid = (String)options.valueOf("uuid"); }

        // TODO: can't run on the command line anymore!
        Worker w = new Worker(configFile, uuid);
        w.start();

    }

    public Worker(String configFile, String vmUuid) {

        settings = u.parseConfig(configFile);
        queueName = (String) settings.get("rabbitMQQueueName");
        this.vmUuid = vmUuid;



    }

    public void run () {

        int max = 1;

        try {

            // the VM UUID
            System.out.println(" WORKER VM UUID: '" + vmUuid + "'");

            // read from
            jobChannel = u.setupQueue(settings, queueName + "_jobs");

            // write to
            resultsChannel = u.setupMultiQueue(settings, queueName+"_results");

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            jobChannel.basicConsume(queueName+"_jobs", false, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (max > 0) {

                System.out.println(" WORKER IS PREPARING TO PULL JOB FROM QUEUE " + vmUuid);

                max--;

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting

                // get the job order
                //int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                //System.out.println("THERE ARE CURRENTLY "+messages+" JOBS QUEUED!");

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                System.out.println(vmUuid + "  received " + delivery.getEnvelope().toString());
                //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody());

                if (message != null) {

                    System.out.println(" [x] Received JOBS REQUEST '" + message + "' @ " + vmUuid);

                    Job job = new Job().fromJSON(message);

                    // TODO: this will obviously get much more complicated when integrated with Docker
                    // launch VM

                    System.out.println(" WORKER LAUNCHING JOB");

                    launchJob(job.getUuid());

                    // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own thread, harvesting STDERR/STDOUT periodically


                    System.out.println(" WORKER FINISHING JOB");

                    finishJob(job.getUuid());

                } else {
                    System.out.println(" [x] Job request came back NULL! ");

                }

                System.out.println(vmUuid + " acknowledges " + delivery.getEnvelope().toString());
                jobChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            }

            System.out.println(" \n\n\nWORKER FOR VM UUID HAS FINISHED!!!: '" + vmUuid + "'\n\n");

            // turns out this is needed when multiple threads are reading from the same
            // queue otherwise you end up with multiple unacknowledged messages being undeliverable to other workers!!!
            jobChannel.close();

            return;

        } catch (Exception ex) {
            System.err.println(ex.toString()); ex.printStackTrace();
        }


        /* catch (IOException ex) {
            System.out.println(ex.toString()); ex.printStackTrace();
        } catch (InterruptedException ex) {
            log.error(ex.toString());
        } catch (ShutdownSignalException ex) {
            log.error(ex.toString());
        } catch (ConsumerCancelledException ex) {
            log.error(ex.toString());
        } */
    }

    // TOOD: obviously, this will need to launch something using Youxia in the future
    private void launchJob(String uuid) {
        try {

            Random random = new Random();
            int min = ((Long) settings.get("min_random_time")).intValue();
            int max = ((Long) settings.get("max_random_time")).intValue();
            int randomNumber = random.nextInt(max - min) + min;

            while(randomNumber > 0) {

                randomNumber--;

                Status s = new Status(vmUuid, uuid, u.RUNNING, u.JOB_MESSAGE_TYPE, "stderr "+randomNumber, "stdout "+randomNumber, "job is running");
                String result = s.toJSON();

                resultsChannel.basicPublish(queueName + "_results", queueName + "_results", MessageProperties.PERSISTENT_TEXT_PLAIN, result.getBytes());

                try {
                    // pause
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.err.println(ex.toString());
                }

            }

        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private void finishJob(String uuid) {
        try {

            Random random = new Random();
            int randomNumber = random.nextInt(100);

            Status s = new Status(vmUuid, uuid, u.SUCCESS, u.JOB_MESSAGE_TYPE, "stderr finished", "stdout finished", "job is finished");
            if (randomNumber < 10) {
                s = new Status(vmUuid, uuid, u.FAILED, u.JOB_MESSAGE_TYPE, "stderr failed", "stdout failed", "job is failed");
            }
            String result = s.toJSON();

            resultsChannel.basicPublish(queueName + "_results", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, result.getBytes());

        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
