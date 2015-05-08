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
 * Created by boconnor on 15-04-18.
 */
public class Worker implements Runnable {

    // private static final int THREAD_POOL_SIZE = 1;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel jobChannel = null;
    // private Connection connection = null;
    private String queueName = null;
    private String jobQueueName;
    private String resultsQueueName;
    private Utilities u = new Utilities();
    private String vmUuid = null;
    private int maxRuns = 1;
    private String userName;
    private static final long QUICK_SLEEP = 50;

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        parser.accepts("uuid").withOptionalArg().ofType(String.class);
        parser.accepts("max-runs").withOptionalArg().ofType(Integer.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        String uuid = null;
        int maxRuns = 1;
        if (options.has("config")) {
            configFile = (String) options.valueOf("config");
        }
        if (options.has("uuid")) {
            uuid = (String) options.valueOf("uuid");
        }
        if (options.has("max-runs")) {
            uuid = (String) options.valueOf("max-runs");
        }

        // TODO: can't run on the command line anymore!
        Worker w = new Worker(configFile, uuid, maxRuns);
        w.run();
        System.out.println("Exiting.");
    }

    public Worker(String configFile, String vmUuid, int maxRuns) {

        this.maxRuns = maxRuns;
        settings = u.parseConfig(configFile);
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
            System.out.println(" WORKER VM UUID: '" + vmUuid + "'");

            // read from
            jobChannel = u.setupQueue(settings, this.jobQueueName);

            // write to
            // TODO: Add some sort of "local debug" mode so that developers working on their local
            // workstation can declare the queue if it doesn't exist. Normally, the results queue is
            // created by the Coordinator.
            resultsChannel = u.setupMultiQueue(settings, this.resultsQueueName);

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            String consumerTag = jobChannel.basicConsume(this.jobQueueName, false, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (max > 0 || maxRuns <= 0) {

                System.out.println(" WORKER IS PREPARING TO PULL JOB FROM QUEUE " + vmUuid);

                max--;

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting

                // get the job order
                // int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                // System.out.println("THERE ARE CURRENTLY "+messages+" JOBS QUEUED!");

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                System.out.println(vmUuid + "  received " + delivery.getEnvelope().toString());
                // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                if (message != null) {

                    System.out.println(" [x] Received JOBS REQUEST '" + message + "' @ " + vmUuid);

                    Job job = new Job().fromJSON(message);

                    // TODO: this will obviously get much more complicated when integrated with Docker
                    // launch VM
                    Status status = new Status(vmUuid, job.getUuid(), StatusState.RUNNING, u.JOB_MESSAGE_TYPE, "", "", "job is starting");
                    String statusJSON = status.toJSON();

                    System.out.println(" WORKER LAUNCHING JOB");
                    // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own
                    // thread, harvesting STDERR/STDOUT periodically
                    String workflowOutput = launchJob(statusJSON, job);

                    launchJob(job.getUuid(), job);

                    status = new Status(vmUuid, job.getUuid(), StatusState.SUCCESS, u.JOB_MESSAGE_TYPE, "", workflowOutput,
                            "job is finished");
                    statusJSON = status.toJSON();

                    System.out.println(" WORKER FINISHING JOB");

                    finishJob(statusJSON);
                } else {
                    System.out.println(" [x] Job request came back NULL! ");
                }
                System.out.println(vmUuid + " acknowledges " + delivery.getEnvelope().toString());
                jobChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            System.out.println(" \n\n\nWORKER FOR VM UUID HAS FINISHED!!!: '" + vmUuid + "'\n\n");
            // turns out this is needed when multiple threads are reading from the same
            // queue otherwise you end up with multiple unacknowledged messages being undeliverable to other workers!!!
            jobChannel.getConnection().close();
            resultsChannel.getConnection().close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
            ex.printStackTrace();
        }
    }

    private Path writeINIFile(Job job) throws IOException {
        System.out.println("INI is: " + job.getIniStr());

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
        FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(perms);
        Path pathToINI = java.nio.file.Files.createTempFile("seqware_", ".ini", attrs);
        System.out.println("INI file: " + pathToINI.toString());
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToINI.toFile()), StandardCharsets.UTF_8));
        bw.write(job.getIniStr());
        bw.flush();
        bw.close();
        // FileWriter writer = new FileWriter(pathToINI.toFile());
        // writer.write(job.getIniStr());
        // writer.close();
        return pathToINI;
    }

    // TODO: obviously, this will need to launch something using Youxia in the future
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

            Status heartbeatStatus = new Status();
            heartbeatStatus.setJobUuid(job.getUuid());
            String networkID = getFirstNonLoopbackAddress().toString();

            heartbeatStatus.setMessage("job is running; IP address: " + networkID);
            heartbeatStatus.setState(StatusState.RUNNING);
            heartbeatStatus.setType(u.JOB_MESSAGE_TYPE);
            heartbeatStatus.setVmUuid(vmUuid);

            WorkerHeartbeat heartbeat = new WorkerHeartbeat();
            heartbeat.setQueueName(this.resultsQueueName);
            heartbeat.setReportingChannel(resultsChannel);
            heartbeat.setSecondsDelay(Double.parseDouble((String) settings.get("heartbeatRate")));
            heartbeat.setMessageBody(heartbeatStatus.toJSON());

            long presleepMillis = Base.ONE_SECOND_IN_MILLISECONDS * Long.parseLong((String) settings.get("preworkerSleep"));
            long postsleepMillis = Base.ONE_SECOND_IN_MILLISECONDS * Long.parseLong((String) settings.get("postworkerSleep"));

            workflowRunner.setCli(cli);
            workflowRunner.setPreworkDelay(presleepMillis);
            workflowRunner.setPostworkDelay(postsleepMillis);

            ExecutorService exService = Executors.newFixedThreadPool(2);
            exService.execute(heartbeat);
            // This short little sleep is only here so that when I run unit tests, the output will be consistent between all executions:
            // FIRST the heartbeat startup output, THEN the workflow runner output.

            Thread.sleep(QUICK_SLEEP);
            Future<String> workflowResult = exService.submit(workflowRunner);
            workflowOutput = workflowResult.get();
            System.out.println("Docker execution result: " + workflowOutput);
            exService.shutdownNow();
            Thread.sleep(QUICK_SLEEP);
        } catch (IOException e) {
            if (workflowRunner.getStdErr() != null) {
                log.error("Error from Docker (stderr): " + workflowOutput);
            } else if (workflowRunner.getStdOut() != null) {
                // maybe the message is in stdout?
                log.error("Error from Docker (stdout): " + workflowOutput);
            } else {
                log.error("Docker experienced an error but did not return any output, error is: " + e.getMessage());
            }
            e.printStackTrace();
            log.error(e.toString());
            return workflowOutput;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return workflowOutput;
    }

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

    private void finishJob(String message) {
        try {
            resultsChannel.basicPublish(this.resultsQueueName, this.resultsQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
