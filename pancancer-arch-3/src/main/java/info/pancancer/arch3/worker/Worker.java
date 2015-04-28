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

import java.io.FilePermission;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    @Override
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
            jobChannel.basicConsume(queueName+"_jobs", true, consumer);


            // TODO: need threads that each read from orders and another that reads results
            while (max > 0) {

                System.out.println(" WORKER IS PREPARING TO PULL JOB FROM QUEUE WITH NAME: "+queueName+"_jobs");

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting

                // get the job order
                //int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                //System.out.println("THERE ARE CURRENTLY "+messages+" JOBS QUEUED!");

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody());

                if (message != null) {

                    max--;

                    System.out.println(" [x] Received JOBS REQUEST '" + message + "'");

                    Job job = new Job().fromJSON(message);

                    System.out.println("INI is: "+job.getIniStr());
                    
                    Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.GROUP_READ);
                    perms.add(PosixFilePermission.GROUP_WRITE);
                    perms.add(PosixFilePermission.GROUP_EXECUTE);
                    perms.add(PosixFilePermission.OTHERS_READ);
                    perms.add(PosixFilePermission.OTHERS_WRITE);
                    perms.add(PosixFilePermission.OTHERS_EXECUTE);
                    FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(perms );
                    Path pathToINI = java.nio.file.Files.createTempFile("seqware_", ".ini", attrs);
                    System.out.println("INI file: "+pathToINI.toString());
                    // TODO: this will obviously get much more complicated when integrated with Docker
                    // launch VM
                    Status s = new Status(vmUuid, job.getUuid(), u.RUNNING, u.JOB_MESSAGE_TYPE, "job is starting");
                    String result = s.toJSON();

                    System.out.println(" WORKER LAUNCHING JOB");
                    // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own thread, harvesting STDERR/STDOUT periodically
                    launchJob(result);


                    // FIXME: this is the source of the bug... this thread never exists and as a consequence it uses the
                    // same VMUUID for all jobs... which mismatches what's in the DB and hence the update in the DB never happens

                    s = new Status(vmUuid, job.getUuid(), u.SUCCESS, u.JOB_MESSAGE_TYPE, "job is finished");
                    result = s.toJSON();

                    System.out.println(" WORKER FINISHING JOB");

                    finishJob(result);

                } else {
                    System.out.println(" [x] Job request came back NULL! ");
                }

            }

            jobChannel.close();
            resultsChannel.close();

            return;


        } catch (Exception ex) {
            System.err.println(ex.toString()); ex.printStackTrace();
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
            resultsChannel.basicPublish(queueName + "_results", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
            //resultsChannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
