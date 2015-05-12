package info.pancancer.arch3.worker;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.utils.Utilities;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.exec.CommandLine;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;

/**
 * This class represents a Worker, in the Architecture 3 design. A Worker can receive job messages from a queue and execute a seqware
 * workflow based on the contents of the job message.
 * 
 * Created by boconnor on 15-04-18.
 */
public class Worker implements Runnable {

    private static final String NO_MESSAGE_FROM_QUEUE_MESSAGE = " [x] Job request came back null/empty! ";
    protected static final Logger LOG = LoggerFactory.getLogger(Worker.class);
    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel jobChannel = null;
    private String queueName = null;
    private String jobQueueName;
    private String resultsQueueName;
    private String vmUuid = null;
    private int maxRuns = 1;
    private String userName;
    private static final long QUICK_SLEEP = 50;

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        parser.accepts("max-runs").withOptionalArg().ofType(Integer.class);
        //UUID should be required: if the user doesn't specify it, there's no other way for the VM to get a UUID
        parser.accepts("uuid").withRequiredArg().ofType(String.class).required();
        OptionSet options = parser.parse(argv);

        String configFile = null;
        String uuid = null;
        int maxRuns = 1;
        if (options.has("config")) {
            configFile = (String) options.valueOf("config");
        }
        if (options.has("max-runs")) {
            maxRuns = (Integer) options.valueOf("max-runs");
        }
        //uuid is REQUIRED, OptionParser will fail if it's missing.
        uuid = (String) options.valueOf("uuid");
        
        // TODO: can't run on the command line anymore!
        Worker w = new Worker(configFile, uuid, maxRuns);
        w.run();
        //System.out.println("Exiting.");
        LOG.info("Exiting.");
    }

    /**
     * Create a new Worker.
     * 
     * @param configFile
     *            - The name of the configuration file to read.
     * @param vmUuid
     *            - The UUID of the VM on which this worker is running.
     * @param maxRuns
     *            - The maximum number of workflows this Worker should execute.
     */
    public Worker(String configFile, String vmUuid, int maxRuns) {

        this.maxRuns = maxRuns;
        settings = Utilities.parseConfig(configFile);
        this.queueName = (String) settings.get("rabbitMQQueueName");
        if (this.queueName == null) {
            throw new NullPointerException(
                    "Queue name was null! Please ensure that you have properly configured \"rabbitMQQueueName\" in your config file.");
        }
        this.jobQueueName = this.queueName + "_jobs";
        this.resultsQueueName = this.queueName + "_results";
        this.userName = (String) settings.get("hostUserName");
        this.vmUuid = vmUuid;
        this.maxRuns = maxRuns;
    }

    @Override
    public void run() {

        int max = maxRuns;

        try {

            // the VM UUID
            //System.out.println(" WORKER VM UUID: '" + vmUuid + "'");
            LOG.info(" WORKER VM UUID: '" + vmUuid + "'");

            // read from
            jobChannel = Utilities.setupQueue(settings, this.jobQueueName);

            // write to
            // TODO: Add some sort of "local debug" mode so that developers working on their local
            // workstation can declare the queue if it doesn't exist. Normally, the results queue is
            // created by the Coordinator.
            resultsChannel = Utilities.setupMultiQueue(settings, this.resultsQueueName);

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            String consumerTag = jobChannel.basicConsume(this.jobQueueName, false, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (max > 0 /*|| maxRuns <= 0*/) {

                LOG.info(" WORKER IS PREPARING TO PULL JOB FROM QUEUE " + vmUuid);

                max--;

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting

                // get the job order
                // int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                // System.out.println("THERE ARE CURRENTLY "+messages+" JOBS QUEUED!");

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                LOG.info(vmUuid + "  received " + delivery.getEnvelope().toString());
                // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                if (delivery.getBody() != null) {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    if (message.trim().length() > 0) {

                        LOG.info(" [x] Received JOBS REQUEST '" + message + "' @ " + vmUuid);

                        Job job = new Job().fromJSON(message);

                        // TODO: this will obviously get much more complicated when integrated with Docker
                        // launch VM
                        Status status = new Status(vmUuid, job.getUuid(), StatusState.RUNNING, Utilities.JOB_MESSAGE_TYPE, "", "",
                                "job is starting");
                        String statusJSON = status.toJSON();

                        LOG.info(" WORKER LAUNCHING JOB");
                        // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own
                        // thread, harvesting STDERR/STDOUT periodically
                        String workflowOutput = launchJob(statusJSON, job);

                        launchJob(job.getUuid(), job);

                        status = new Status(vmUuid, job.getUuid(), StatusState.SUCCESS, Utilities.JOB_MESSAGE_TYPE, "", workflowOutput,
                                "job is finished");
                        statusJSON = status.toJSON();

                        LOG.info(" WORKER FINISHING JOB");

                        finishJob(statusJSON);
                    } else {
                        LOG.info(NO_MESSAGE_FROM_QUEUE_MESSAGE);
                    }
                    LOG.info(vmUuid + " acknowledges " + delivery.getEnvelope().toString());
                    jobChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } else {
                    LOG.info(NO_MESSAGE_FROM_QUEUE_MESSAGE);
                }
            }
            LOG.info(" \n\n\nWORKER FOR VM UUID HAS FINISHED!!!: '" + vmUuid + "'\n\n");
            // turns out this is needed when multiple threads are reading from the same
            // queue otherwise you end up with multiple unacknowledged messages being undeliverable to other workers!!!
            jobChannel.getConnection().close();
            resultsChannel.getConnection().close();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(),ex);
            ex.printStackTrace();
        }
    }

    /**
     * Write the content of the job object to an INI file which will be used by the workflow.
     * 
     * @param job
     *            - the job object which must contain a HashMap, which will be used to write an INI file.
     * @return A Path object pointing to the new file will be returned.
     * @throws IOException
     */
    private Path writeINIFile(Job job) throws IOException {
        LOG.info("INI is: " + job.getIniStr());

        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.addAll(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE));
        FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(perms);
        Path pathToINI = java.nio.file.Files.createTempFile("seqware_", ".ini", attrs);
        LOG.info("INI file: " + pathToINI.toString());
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToINI.toFile()), StandardCharsets.UTF_8));
        bw.write(job.getIniStr());
        bw.flush();
        bw.close();
        return pathToINI;
    }

    // TODO: obviously, this will need to launch something using Youxia in the future
    /**
     * This function will execute a workflow, based on the content of the Job object that is passed in.
     * 
     * @param message
     *            - The message that will be published on the queue when the worker starts running the job.
     * @param job
     *            - The job contains information about what workflow to execute, and how.
     * @return The complete stdout and stderr from the workflow execution will be returned.
     */
    private String launchJob(String message, Job job) {
        String workflowOutput = "";

        WorkflowRunner workflowRunner = new WorkflowRunner();
        try {

            Path pathToINI = writeINIFile(job);
            resultsChannel.basicPublish(this.resultsQueueName, this.resultsQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));

            CommandLine cli = new CommandLine("docker");
            cli.addArguments(new String[] { "run", "--rm", "-h", "master", "-t", "-v", "/var/run/docker.sock:/var/run/docker.sock", "-v",
                    job.getWorkflowPath() + ":/workflow", "-v", pathToINI + ":/ini", "-v", "/datastore:/datastore", "-v",
                    "/home/" + this.userName + "/.ssh/gnos.pem:/home/ubuntu/.ssh/gnos.pem", "seqware/seqware_whitestar_pancancer",
                    "seqware", "bundle", "launch", "--dir", "/workflow", "--ini", "/ini", "--no-metadata" });

            WorkerHeartbeat heartbeat = new WorkerHeartbeat();
            heartbeat.setQueueName(this.resultsQueueName);
            heartbeat.setReportingChannel(resultsChannel);
            heartbeat.setSecondsDelay(Double.parseDouble((String) settings.get("heartbeatRate")));
            heartbeat.setJobUuid(job.getUuid());
            heartbeat.setVmUuid(this.vmUuid);
            heartbeat.setNetworkID(getFirstNonLoopbackAddress().toString());
            heartbeat.setStatusSource(workflowRunner);
            // heartbeat.setMessageBody(heartbeatStatus.toJSON());

            long presleepMillis = Base.ONE_SECOND_IN_MILLISECONDS * Long.parseLong((String) settings.get("preworkerSleep"));
            long postsleepMillis = Base.ONE_SECOND_IN_MILLISECONDS * Long.parseLong((String) settings.get("postworkerSleep"));

            workflowRunner.setCli(cli);
            workflowRunner.setPreworkDelay(presleepMillis);
            workflowRunner.setPostworkDelay(postsleepMillis);

            ExecutorService exService = Executors.newFixedThreadPool(2);
            exService.execute(heartbeat);
            Future<String> workflowResult = exService.submit(workflowRunner);
            workflowOutput = workflowResult.get();
            LOG.info("Docker execution result: " + workflowOutput);
            exService.shutdownNow();
        } catch (SocketException e) {
            // This comes from trying to get the IP address.
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            // This could be caused by a problem writing the file, or publishing a message to the queue.
            LOG.error(e.getMessage(), e);
        } catch (ExecutionException | InterruptedException e) {
            // This comes from trying to get the workflow execution result.
            LOG.error("Error executing workflow: " + e.getMessage(), e);
        }
        return workflowOutput;
    }

    /**
     * Get the IP address of this machine, preference is given to returning an IPv4 address, if there is one.
     * 
     * @return An InetAddress object.
     * @throws SocketException
     */
    private static InetAddress getFirstNonLoopbackAddress() throws SocketException {
        for (NetworkInterface i : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress addr : Collections.list(i.getInetAddresses())) {
                if (!addr.isLoopbackAddress()) {
                    // Prefer IP v4
                    if (addr instanceof Inet4Address) {
                        return addr;
                    }
                }

            }
            // If we got here it means we never found an IP v4 address, so we'll have to return the IPv6 address.
            for (InetAddress addr : Collections.list(i.getInetAddresses())) {
                // InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    return addr;
                }
            }
        }
        return null;
    }

    /**
     * Publish a message stating that the job is finished.
     * 
     * @param message
     *            - The actual message to publish.
     */
    private void finishJob(String message) {
        try {
            resultsChannel.basicPublish(this.resultsQueueName, this.resultsQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

}
