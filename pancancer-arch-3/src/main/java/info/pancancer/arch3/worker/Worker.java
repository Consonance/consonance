package info.pancancer.arch3.worker;

import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.utils.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Created by boconnor on 15-04-18.
 */
public class Worker implements Runnable {

    private static final String CHARSET_ENCODING = "UTF-8";
    private static final int THREAD_POOL_SIZE = 1;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel jobChannel = null;
    private Connection connection = null;
    private String queueName = null;
    private Utilities u = new Utilities();
    private String vmUuid = null;
    private int maxRuns = 1;

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

        Worker w = new Worker(configFile, uuid, maxRuns);
        w.run();
        System.out.println("Exiting.");
    }

    public Worker(String configFile, String vmUuid, int maxRuns) {

        this.maxRuns = maxRuns;
        settings = u.parseConfig(configFile);
        queueName = (String) settings.get("rabbitMQQueueName");
        this.vmUuid = vmUuid;

    }

    @Override
    public void run() {

        int max = maxRuns;

        try {

            // the VM UUID
            System.out.println(" WORKER VM UUID: '" + vmUuid + "'");

            // read from

            jobChannel = u.setupQueue(settings, queueName + "_jobs");
            // write to
            resultsChannel = u.setupMultiQueue(settings, queueName + "_results");

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            String consumerTag = jobChannel.basicConsume(queueName + "_jobs", false, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (max > 0 || maxRuns <0 ) {

                System.out.println(" WORKER IS PREPARING TO PULL JOB FROM QUEUE WITH NAME: " + queueName + "_jobs");

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting

                // get the job order
                // int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                // System.out.println("THERE ARE CURRENTLY "+messages+" JOBS QUEUED!");

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message;
                message = new String(delivery.getBody());

                if ("".equals(message) || message.length() > 0) {

                    max--;

                    System.out.println(" [x] Received JOBS REQUEST '" + message + "' @ "+vmUuid);

                    Job job = new Job().fromJSON(message);

                    // TODO: this will obviously get much more complicated when integrated with Docker
                    // launch VM
                    Status status = new Status(vmUuid, job.getUuid(), u.RUNNING, u.JOB_MESSAGE_TYPE, "job is starting");
                    String statusJSON = status.toJSON();

                    System.out.println(" WORKER LAUNCHING JOB");
                    // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own
                    // thread, harvesting STDERR/STDOUT periodically
                    String workflowOutput = launchJob(statusJSON, job);

                    // FIXME: this is the source of the bug... this thread never exists and as a consequence it uses the
                    // same VMUUID for all jobs... which mismatches what's in the DB and hence the update in the DB never happens

                    status = new Status(vmUuid, job.getUuid(), u.SUCCESS, u.JOB_MESSAGE_TYPE, "job is finished, Workflow result is:\n\n"+workflowOutput);
                    statusJSON = status.toJSON();

                    System.out.println(" WORKER FINISHING JOB");

                    finishJob(statusJSON);
                } else {
                    System.out.println(" [x] Job request came back NULL! ");
                }
                System.out.println(vmUuid + " acknowledges " + delivery.getEnvelope().toString());
                jobChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            jobChannel.getConnection().close();
            resultsChannel.getConnection().close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
            ex.printStackTrace();
        }
        // this.notify();
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
        FileWriter writer = new FileWriter(pathToINI.toFile());
        writer.write(job.getIniStr());
        writer.close();
        return pathToINI;
    }

    // TODO: obviously, this will need to launch something using Youxia in the future
    private String launchJob(String message, Job job) {
        String workflowOutput = "";
        String cmdResponse = null;
        /*LogOutputStream outputStream = new LogOutputStream() {
            private final List<String> lines = new LinkedList<String>();
            @Override protected void processLine(String line, int level) {
                lines.add(line);
            }   
            public List<String> getLines() {
                return lines;
            }
            
            public String toString()
            {
                StringBuffer buff = new StringBuffer();
                for (String l : this.lines)
                {
                    buff.append(l);
                }
                return buff.toString();
            }
        };*/
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        try {
            Path pathToINI = writeINIFile(job);
            resultsChannel.basicPublish(queueName + "_results", queueName + "_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

            // Now we need to launch seqware in docker.
            DefaultExecutor executor = new DefaultExecutor();

            CommandLine cli = new CommandLine("docker");
            cli.addArguments(new String[] { "run", "--rm", "-h", "master", "-t", "-v", job.getWorkflowPath() + ":/workflow", "-v",
                    pathToINI + ":/ini","-v","/datastore:/datastore", /*"-i",*/ "seqware/seqware_whitestar_pancancer", "seqware", "bundle", "launch", "--dir", "/workflow",
                    "--ini", "/ini", "--no-metadata"});
            System.out.println("Executing command: " + cli.toString().replace(",", ""));

            Status heartbeatStatus = new Status();
            heartbeatStatus.setJobUuid(job.getUuid());
            String networkID = getFirstNonLoopbackAddress().toString();
            
            heartbeatStatus.setMessage("job is running; IP address: "+networkID);
            heartbeatStatus.setState(u.RUNNING);
            heartbeatStatus.setType(u.JOB_MESSAGE_TYPE);
            heartbeatStatus.setVmUuid(vmUuid);
            
            WorkerHeartbeat heartbeat = new WorkerHeartbeat();
            heartbeat.setQueueName(this.queueName);
            heartbeat.setReportingChannel(resultsChannel);
            heartbeat.setSecondsDelay(Double.parseDouble((String)settings.get("heartbeatRate")));
            heartbeat.setMessageBody(heartbeatStatus.toJSON());
            
            ExecutorService exService = Executors.newSingleThreadExecutor();
            exService.execute(heartbeat);
            
            executor.setStreamHandler(streamHandler);
            
            executor.execute(cli);
            long presleepMillis = 1000 * Long.parseLong((String)settings.get("preworkerSleep"));
            if (presleepMillis>0)
            {
                System.out.println("Sleeping before executing workflow for "+presleepMillis+" ms.");
                Thread.sleep(presleepMillis);
            }
            workflowOutput = new String(outputStream.toByteArray());
            System.out.println("Docker execution result: " + workflowOutput);
            long postsleepMillis = 1000 * Long.parseLong((String)settings.get("postworkerSleep"));
            if (postsleepMillis>0)
            {
                System.out.println("Sleeping after exeuting workflow for "+postsleepMillis+ " ms.");
                Thread.sleep(postsleepMillis);
            }
            exService.shutdownNow();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            // if (cmdResponse!=null)
            // cmdResponse.toString();
            if (outputStream != null) {
                workflowOutput = new String(outputStream.toByteArray());
                log.error("Error from Docker: " + workflowOutput);
            }
            e.printStackTrace();
            log.error(e.toString());
            return workflowOutput;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return workflowOutput;
    }
    private static InetAddress getFirstNonLoopbackAddress() throws SocketException {
//        Enumeration en = NetworkInterface.getNetworkInterfaces();
//        while (en.hasMoreElements()) {
//            NetworkInterface i = (NetworkInterface) en.nextElement();
        for (NetworkInterface i : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            //for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();)
            for (InetAddress addr : Collections.list(i.getInetAddresses()))
            {
                //InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                        return addr;
                }
            }
        }
        return null;
    }
    private void finishJob(String message) {
        try {
            resultsChannel.basicPublish(queueName+"_results", queueName + "_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
