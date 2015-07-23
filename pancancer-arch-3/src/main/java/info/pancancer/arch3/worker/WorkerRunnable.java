package info.pancancer.arch3.worker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.exec.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import io.cloudbindle.youxia.util.Log;

/**
 * This class represents a WorkerRunnable, in the Architecture 3 design.
 *
 * A WorkerRunnable can receive job messages from a queue and execute a seqware workflow based on the contents of the job message. Created
 * by boconnor on 15-04-18.
 */
public class WorkerRunnable implements Runnable {

    private static final String NO_MESSAGE_FROM_QUEUE_MESSAGE = " [x] Job request came back null/empty! ";
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private HierarchicalINIConfiguration settings = null;
    private Channel resultsChannel = null;
    private String queueName = null;
    private String jobQueueName;
    private String resultsQueueName;
    private String vmUuid = null;
    private int maxRuns = 1;
    private String userName;
    private boolean testMode;
    private boolean endless = false;
    private boolean workflowFailed = false;
    public static final int DEFAULT_PRESLEEP = 1;
    public static final int DEFAULT_POSTSLEEP = 1;
    //private static final int FIVE_SECONDS_IN_MS = 5000;
    private String networkAddress;

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
    public WorkerRunnable(String configFile, String vmUuid, int maxRuns) {
        this(configFile, vmUuid, maxRuns, false, false);
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
     * @param testMode
     *            - Should this worker run in testMode (seqware job will not actually be launched)
     * @param endless
     *            - have the worker pick up new jobs as the current job finishes successfully
     */
    public WorkerRunnable(String configFile, String vmUuid, int maxRuns, boolean testMode, boolean endless) {
        log.debug("WorkerRunnable created with args:\n\tconfigFile: " + configFile + "\n\tvmUuid: " + vmUuid + "\n\tmaxRuns: " + maxRuns
                + "\n\ttestMode: " + testMode + "\n\tendless: " + endless);

        try {
            this.networkAddress = getFirstNonLoopbackAddress().toString().substring(1);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            log.error("Could not get network address: " + e.getMessage(), e);
            throw new RuntimeException("Could not get network address: " + e.getMessage());
        }

        this.maxRuns = maxRuns;
        settings = Utilities.parseConfig(configFile);

        // TODO: Dyanmically change path to log file, it should be /var/log/arch3.log in production, but for test, ./arch3.log
        // FileAppender<ILoggingEvent> appender = (FileAppender<ILoggingEvent>)
        // ((ch.qos.logback.classic.Logger)log).getAppender("FILE_APPENDER");
        // appender.setFile("SomePath");

        this.queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
        if (this.queueName == null) {
            throw new NullPointerException(
                    "Queue name was null! Please ensure that you have properly configured \"rabbitMQQueueName\" in your config file.");
        }
        this.jobQueueName = this.queueName + "_jobs";
        this.resultsQueueName = this.queueName + "_results";
        this.userName = settings.getString(Constants.WORKER_HOST_USER_NAME, "ubuntu");
        /*
         * If the user specified "--endless" on the CLI, then this.endless=true Else: check to see if "endless" is in the config file, and
         * if it is, parse the value of it and use that. If not in the config file, then use "false".
         */
        this.endless = endless ? endless : settings.getBoolean(Constants.WORKER_ENDLESS, false);
        if (this.endless) {
            log.info("The \"--endless\" flag was set, this worker will run endlessly!");
        }
        this.vmUuid = vmUuid;
        this.maxRuns = maxRuns;
        this.testMode = testMode;
    }

    @Override
    public void run() {

        int max = maxRuns;

        try {
            // the VM UUID
            log.info(" WORKER VM UUID provided as: '" + vmUuid + "'");
            // write to
            // TODO: Add some sort of "local debug" mode so that developers working on their local
            // workstation can declare the queue if it doesn't exist. Normally, the results queue is
            // created by the Coordinator.
            resultsChannel = Utilities.setupExchange(settings, this.resultsQueueName);

            while ((max > 0 || this.endless) && !this.workflowFailed) {
                log.debug(max + " remaining jobs will be executed");
                log.info(" WORKER IS PREPARING TO PULL JOB FROM QUEUE " + this.jobQueueName);

                if (!endless) {
                    max--;
                }

                // jobChannel needs to be created inside the loop because it is closed inside the loop, and it is closed inside this loop to
                // prevent pre-fetching.
                Channel jobChannel = Utilities.setupQueue(settings, this.jobQueueName);
                if (jobChannel == null) {
                    throw new NullPointerException("jobChannel is null for queue: " + this.jobQueueName
                            + ". Something bad must have happened while trying to set up the queue connections. Please ensure that your configuration is correct.");
                }
                QueueingConsumer consumer = new QueueingConsumer(jobChannel);
                jobChannel.basicConsume(this.jobQueueName, false, consumer);

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                log.info(vmUuid + "  received " + delivery.getEnvelope().toString());
                if (delivery.getBody() != null) {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    if (message.trim().length() > 0) {

                        log.info(" [x] Received JOBS REQUEST '" + message + "' @ " + vmUuid);

                        Job job = new Job().fromJSON(message);

                        Status status = new Status(vmUuid, job.getUuid(), StatusState.RUNNING, Utilities.JOB_MESSAGE_TYPE,
                                "job is starting", this.networkAddress);
                        status.setStderr("");
                        status.setStdout("");
                        String statusJSON = status.toJSON();

                        log.info(" WORKER LAUNCHING JOB");

                        // greedy acknowledge, it will be easier to deal with lost jobs than zombie workers in hostile OpenStack
                        // environments
                        log.info(vmUuid + " acknowledges " + delivery.getEnvelope().toString());
                        jobChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        // we need to close the channel IMMEDIATELY to complete the ACK.
                        jobChannel.close();
                        // Close the connection object as well, or the main thread may not exit because of still-open-and-in-use resources.
                        jobChannel.getConnection().close();

                        WorkflowResult workflowResult = new WorkflowResult();
                        if (testMode) {
                            workflowResult.setWorkflowStdout("everything is awesome");
                            workflowResult.setExitCode(0);
                        } else {
                            String seqwareEngine = settings.getString(Constants.WORKER_SEQWARE_ENGINE, Constants.SEQWARE_WHITESTAR_ENGINE);
                            String seqwareSettingsFile = settings.getString(Constants.WORKER_SEQWARE_SETTINGS_FILE);
                            workflowResult = launchJob(statusJSON, job, seqwareEngine, seqwareSettingsFile);
                        }

                        status = new Status(vmUuid, job.getUuid(),
                                workflowResult.getExitCode() == 0 ? StatusState.SUCCESS : StatusState.FAILED, Utilities.JOB_MESSAGE_TYPE,
                                "job is finished", networkAddress);
                        status.setStderr(workflowResult.getWorkflowStdErr());
                        status.setStdout(workflowResult.getWorkflowStdout());
                        statusJSON = status.toJSON();

                        log.info(" WORKER FINISHING JOB");

                        finishJob(statusJSON);
                    } else {
                        log.info(NO_MESSAGE_FROM_QUEUE_MESSAGE);
                    }
                    // we need to close the channel *conditionally*
                    if (jobChannel.isOpen()) {
                        jobChannel.close();
                    }
                    // Close the connection object as well, or the main thread may not exit because of still-open-and-in-use resources.
                    if (jobChannel.getConnection().isOpen()) {
                        jobChannel.getConnection().close();
                    }
                } else {
                    log.info(NO_MESSAGE_FROM_QUEUE_MESSAGE);
                }
            }
            log.info(" \n\n\nWORKER FOR VM UUID HAS FINISHED!!!: '" + vmUuid + "'\n\n");
            if (this.workflowFailed) {
                log.error(
                        "The last workflow executed by this Worker did not complete successfully. No more workflows will be attempted. Please check the logs and fix any problems before trying another workflow.");
            }
            // turns out this is needed when multiple threads are reading from the same
            // queue otherwise you end up with multiple unacknowledged messages being undeliverable to other workers!!!
            if (resultsChannel != null && resultsChannel.isOpen()) {
                resultsChannel.close();
                resultsChannel.getConnection().close();
            }
            log.debug("result channel open: " + resultsChannel.isOpen());
            log.debug("result channel connection open: " + resultsChannel.getConnection().isOpen());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
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
        log.info("INI is: " + job.getIniStr());
        EnumSet<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE);
        FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(perms);
        Path pathToINI = Files.createTempFile("seqware_", ".ini", attrs);
        log.info("INI file: " + pathToINI.toString());
        Files.write(pathToINI, job.getIniStr().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
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
    private WorkflowResult launchJob(String message, Job job, String seqwareEngine, String seqwareSettingsFile) {
        WorkflowResult workflowResult = null;
        ExecutorService exService = Executors.newFixedThreadPool(2);
        WorkflowRunner workflowRunner = new WorkflowRunner();
        try {

            Path pathToINI = writeINIFile(job);
            resultsChannel.basicPublish(this.resultsQueueName, this.resultsQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));
            resultsChannel.waitForConfirms();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_z");
            String timestampStr = sdf.format(new Date());

            String containerIDFile = "/home/" + this.userName + "/worker_" + timestampStr + ".cid";
            String dockerImage = "pancancer/seqware_whitestar_pancancer:1.1.1";

            List<String> args = new ArrayList<String>(Arrays.asList("--cidfile=\"" + containerIDFile + "\"", "-h", "master", "-t", "-v",
                    "/var/run/docker.sock:/var/run/docker.sock", "-v", job.getWorkflowPath() + ":/workflow", "-v", pathToINI + ":/ini",
                    "-v", "/datastore:/datastore", "-v", "/home/" + this.userName + "/.gnos:/home/ubuntu/.gnos"));
            if (seqwareSettingsFile != null) {
                args.addAll(Arrays.asList("-v", seqwareSettingsFile + ":/home/seqware/.seqware/settings"));
            }
            args.addAll(Arrays.asList(dockerImage, "seqware", "bundle", "launch", "--dir", "/workflow", "--ini", "/ini", "--no-metadata",
                    "--engine", seqwareEngine));

            // String runnerPath = this.writeDockerRunnerScript(args);
            CommandLine cli = new CommandLine("docker run");
            cli.addArguments(args.toArray(new String[args.size()]),false);

            WorkerHeartbeat heartbeat = new WorkerHeartbeat();
            heartbeat.setQueueName(this.resultsQueueName);
            // channels should not be shared between threads https://www.rabbitmq.com/api-guide.html#channel-threads
            // heartbeat.setReportingChannel(resultsChannel);
            heartbeat.setSettings(settings);
            heartbeat.setSecondsDelay(settings.getDouble(Constants.WORKER_HEARTBEAT_RATE, WorkerHeartbeat.DEFAULT_DELAY));
            heartbeat.setJobUuid(job.getUuid());
            heartbeat.setVmUuid(this.vmUuid);
            heartbeat.setNetworkID(this.networkAddress);
            heartbeat.setStatusSource(workflowRunner);

            long presleep = settings.getLong(Constants.WORKER_PREWORKER_SLEEP, WorkerRunnable.DEFAULT_PRESLEEP);
            long postsleep = settings.getLong(Constants.WORKER_POSTWORKER_SLEEP, WorkerRunnable.DEFAULT_POSTSLEEP);
            long presleepMillis = Base.ONE_SECOND_IN_MILLISECONDS * presleep;
            long postsleepMillis = Base.ONE_SECOND_IN_MILLISECONDS * postsleep;

            workflowRunner.setCli(cli);
            workflowRunner.setPreworkDelay(presleepMillis);
            workflowRunner.setPostworkDelay(postsleepMillis);
            // Submit both
            @SuppressWarnings("unused")
            // We will never actually do submit.get(), because the heartbeat should keep running until it is terminated by
            // exService.shutdownNow().
            Future<?> submit = exService.submit(heartbeat);
            Future<WorkflowResult> workflowResultFuture = exService.submit(workflowRunner);
            // make sure both are complete
            workflowResult = workflowResultFuture.get();
            // don't get the heartbeat if the workflow is complete already

            log.info("Docker execution result: " + workflowResult.getWorkflowStdout());

            // Now that the work is finished, we have to manage the CID files.
            this.copyCID(containerIDFile, workflowResult.getExitCode());

        } catch (SocketException e) {
            // This comes from trying to get the IP address.
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            // This could be caused by a problem writing the file, or publishing a message to the queue.
            log.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            log.error("Error executing workflow: " + e.getMessage(), e);
            this.workflowFailed = true;
        } catch (InterruptedException e) {
            log.error("Workflow may have been interrupted: " + e.getMessage(), e);
            this.workflowFailed = true;
        } finally {
            exService.shutdownNow();
        }

        if (workflowResult == null || (workflowResult != null && workflowResult.getExitCode() != 0)) {
            this.workflowFailed = true;
        }

        return workflowResult;
    }

    private void copyCID(String containerIDFile, int exitCode) {
        try {
            Path p = Paths.get(new URI("file://"+containerIDFile));
            File f = p.toFile();
            Reader in = new FileReader(f);
            try (BufferedReader reader = new BufferedReader(in)) {
                String buffer = reader.readLine();
                // Now we have to append the CID to the success file or failure file, dependning on the exit code.
                if (exitCode == 0) {
                    Path pathToSuccessfulCID = Paths.get(new URI("/home/ubuntu/successful_container_cids"));
                    OutputStream outStream = Files.newOutputStream(pathToSuccessfulCID, StandardOpenOption.APPEND,
                            StandardOpenOption.CREATE);
                    outStream.write(buffer.getBytes());
                    outStream.close();
                } else {
                    Path pathToUnsuccessfulCID = Paths.get(new URI("/home/ubuntu/unsuccessful_container_cids"));
                    OutputStream outStream = Files.newOutputStream(pathToUnsuccessfulCID, StandardOpenOption.APPEND,
                            StandardOpenOption.CREATE);
                    outStream.write(buffer.getBytes());
                    outStream.close();
                }
            }
        } catch (URISyntaxException e) {
            log.error("Bad URI for container ID file: " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            log.error("Could not find container ID file: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error reading container ID file: " + e.getMessage(), e);
        }

    }

    /**
     * Get the IP address of this machine, preference is given to returning an IPv4 address, if there is one.
     *
     * @return An InetAddress object.
     * @throws SocketException
     */
    public InetAddress getFirstNonLoopbackAddress() throws SocketException {
        final String dockerInterfaceName = "docker";
        for (NetworkInterface i : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (i.getName().contains(dockerInterfaceName)) {
                // the virtual ip address for the docker mount is useless but is not a loopback address
                continue;
            }
            log.info("Examining " + i.getName());
            for (InetAddress addr : Collections.list(i.getInetAddresses())) {
                if (!addr.isLoopbackAddress()) {
                    // Prefer IP v4
                    if (addr instanceof Inet4Address) {
                        return addr;
                    }
                }

            }
        }
        Log.info("Could not find an ipv4 address");
        for (NetworkInterface i : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            log.info("Examining " + i.getName());
            if (i.getName().contains(dockerInterfaceName)) {
                // the virtual ip address for the docker mount is useless but is not a loopback address
                continue;
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
        log.info("Publishing worker results to results channel " + this.resultsQueueName + ": " + message);
        try {
            boolean success = false;
            do {
                try {
                    resultsChannel.basicPublish(this.resultsQueueName, this.resultsQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                            message.getBytes(StandardCharsets.UTF_8));
                    resultsChannel.waitForConfirms();
                    success = true;
                } catch (AlreadyClosedException e) {
                    // retry indefinitely if the connection is down
                    log.error("could not send closed message, retrying", e);
                    Thread.sleep(Base.ONE_MINUTE_IN_MILLISECONDS);
                }
            } while (!success);

        } catch (IOException | InterruptedException e) {
            log.error(e.toString());
        }
        log.info("Finished job report, let's call it a day");
    }
}
