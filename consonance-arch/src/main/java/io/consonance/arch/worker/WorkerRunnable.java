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

package io.consonance.arch.worker;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import io.cloudbindle.youxia.util.Log;
import io.consonance.arch.Base;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.Status;
import io.consonance.arch.beans.StatusState;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private boolean testMode;
    private boolean endless = false;
    public static final int DEFAULT_PRESLEEP = 1;
    public static final int DEFAULT_POSTSLEEP = 1;
    private String networkAddress;
    private String flavour = null;

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
        this(configFile, vmUuid, maxRuns, false, false, null);
    }

    /**
     * Create a new Worker.
     *
     * @param configFile The name of the configuration file to read.
     * @param vmUuid The UUID of the VM on which this worker is running.
     * @param maxRuns The maximum number of workflows this Worker should execute.
     * @param testMode Should this worker run in testMode (seqware job will not actually be launched)
     * @param endless have the worker pick up new jobs as the current job finishes successfully
     * @param flavourOverride override detection of instance type
     */
    public WorkerRunnable(String configFile, String vmUuid, int maxRuns, boolean testMode, boolean endless, String flavourOverride) {

        try {

            log.debug("WorkerRunnable created with args:\n\tconfigFile: " + configFile + "\n\tvmUuid: " + vmUuid + "\n\tmaxRuns: " + maxRuns
                    + "\n\ttestMode: " + testMode + "\n\tendless: " + endless);

            int tries = Base.DEFAULT_NETWORK_RETRIES;
            while(tries > 0) {
                try {
                    this.networkAddress = getFirstNonLoopbackAddress().toString().substring(1);
                    tries = 0;
                } catch (SocketException e) {
                    tries--;
                    Thread.sleep(Base.FIVE_SECOND_IN_MILLISECONDS);
                    // TODO Auto-generated catch block
                    log.error("Could not get network address: " + e.getMessage(), e);
                    //FIXME: this is a problem since an exception here would cause the worker daemon to exit with no info being sent back to the master
                    //throw new RuntimeException("Could not get network address: " + e.getMessage());
                }
            }

            this.maxRuns = maxRuns;
            settings = CommonTestUtilities.parseConfig(configFile);

            // TODO: Dynamically change path to log file, it should be /var/log/arch3.log in production, but for test, ./arch3.log
            // FileAppender<ILoggingEvent> appender = (FileAppender<ILoggingEvent>)
            // ((ch.qos.logback.classic.Logger)log).getAppender("FILE_APPENDER");
            // appender.setFile("SomePath");

            this.queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
            if (this.queueName == null) {
                //throw new NullPointerException(
                //        "Queue name was null! Please ensure that you have properly configured \"rabbitMQQueueName\" in your config file.");
                // FIXME: this is a problem since an exception here would cause the worker daemon to exit with no info being sent back to the master
                log.error("Queue name was null! Please ensure that you have properly configured \"rabbitMQQueueName\" in your config file.");
            }
            this.jobQueueName = this.queueName + "_jobs";
            this.resultsQueueName = this.queueName + "_results";
            /*
             * If the user specified "--endless" on the CLI, then this.endless=true Else: check to see if "endless" is in the config file, and
             * if it is, parse the value of it and use that. If not in the config file, then use "false".
             */
            this.endless = endless || settings.getBoolean(Constants.WORKER_ENDLESS, false);
            if (this.endless) {
                log.info("The \"--endless\" flag was set, this worker will run endlessly!");
            }
            this.vmUuid = vmUuid;
            this.maxRuns = maxRuns;
            this.testMode = testMode;
            this.flavour = flavourOverride;

        } catch (Exception e) {
            log.error("There was a problem in the WorkerRunnable constructor!!! The worker daemon is likely to not work properly!!! "+e.getMessage(), e);
        }
    }

    @Override
    public void run() {

        int max = maxRuns;
        WorkflowResult workflowResult = null;
        String statusJSON = null;
        Job job = null;

        try {

            // worker will need to pull from a specific queue
            // for aws: curl http://169.254.169.254/latest/meta-data/instance-type
            // for openstack: curl http://169.254.169.254/latest/meta-data/instance-type

            // the VM UUID
            log.info(" WORKER VM UUID provided as: '" + vmUuid + "'");

            HttpClient client = new DefaultHttpClient();
            // make really sure that we get a flavour
            while (flavour == null) {
                String responseBody;
                // if no OpenStack uuid is found, grab a normal instance_id from AWS
                String instanceTypeURL = "http://169.254.169.254/latest/meta-data/instance-type";
                final HttpGet method = new HttpGet(instanceTypeURL);
                try {
                    final HttpResponse execute = client.execute(method);
                    responseBody = IOUtils.toString(execute.getEntity().getContent(), StandardCharsets.UTF_8);
                    if (responseBody != null && execute.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        flavour = responseBody;
                        log.info(" flavour chosen using cloud ini meta-data as: '" + flavour + "'");
                    }
                } catch (IOException ioe) {
                    Log.warn("Unable to connect to '" + instanceTypeURL + "'");
                }
            }

            // write to
            // TODO: Add some sort of "local debug" mode so that developers working on their local
            // workstation can declare the queue if it doesn't exist. Normally, the results queue is
            // created by the Coordinator.
            resultsChannel = CommonServerTestUtilities.setupExchange(settings, this.resultsQueueName);

            // variables
            job = null;

            while ((max > 0 || this.endless)) {
                log.debug(max + " remaining jobs will be executed");
                log.info(" WORKER IS PREPARING TO PULL JOB FROM QUEUE " + this.jobQueueName + " for flavour " + this.flavour);

                if (!endless) {
                    max--;
                }

                // Do the actual work
                processJobMessage(workflowResult, statusJSON, job);

            }

            log.info(" \n\n\nWORKER FOR VM UUID HAS FINISHED!!!: '" + vmUuid + "'\n\n");
            // turns out this is needed when multiple threads are reading from the same
            // queue otherwise you end up with multiple unacknowledged messages being undeliverable to other workers!!!
            if (resultsChannel != null && resultsChannel.isOpen()) {
                resultsChannel.close();
                resultsChannel.getConnection().close();
            }
            log.debug("result channel open: " + (resultsChannel != null ? resultsChannel.isOpen() : null));
            log.debug("result channel connection open: " + (resultsChannel != null ? resultsChannel.getConnection().isOpen() : null));

        } catch (Exception ex) {

            log.error("EXCEPTION IN WORKER THREAD!!!!" + ex.getMessage(), ex);

            try {
                // any problems should trigger the failure of this workflow?
                // TODO: make sure variables are in scope
                Status status = new Status(vmUuid, job.getUuid(),
                        StatusState.FAILED, CommonServerTestUtilities.JOB_MESSAGE_TYPE,
                        "job is failed", networkAddress);
                status.setStderr(workflowResult.getWorkflowStdErr());
                status.setStdout(workflowResult.getWorkflowStdout());
                statusJSON = status.toJSON();

                log.error(" WORKER FAILED JOB");

                finishJob(statusJSON);
            } catch (Exception e) {
                log.error("EXCEPTION IN WORKER THREAD ATTEMPTING TO WRITE BACK FAILURE TO DB!!!  THE WORKER DAEMON WAS ATTEMPTING TO REPORT BACK A FAILED WORKFLOW AND THIS HAPPENED: " + ex.getMessage(), ex);
            }

        }
    }

    /**
     * The method for actually processing a job
     * @param workflowResult
     * @param statusJSON
     * @param job
     * @throws Exception
     */
    private void processJobMessage(WorkflowResult workflowResult, String statusJSON, Job job) throws Exception {

        // jobChannel needs to be created inside the loop because it is closed inside the loop, and it is closed inside this loop to
        // prevent pre-fetching.

        // create the job exchange
        String exchange = queueName + "_job_exchange";
        Channel jobChannel = CommonServerTestUtilities.setupExchange(settings, exchange, "direct");
        if (jobChannel == null) {
            throw new NullPointerException("jobChannel is null for queue: " + this.jobQueueName
                    + ". Something bad must have happened while trying to set up the queue connections. Please ensure that your configuration is correct.");
        }
        final String finalQueueName = CommonServerTestUtilities.setupQueueOnExchange(jobChannel, queueName + "_jobs", flavour);
        jobChannel.queueBind(finalQueueName, exchange,flavour);

        QueueingConsumer consumer = new QueueingConsumer(jobChannel);
        jobChannel.basicConsume(finalQueueName, false, consumer);

        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        log.info(vmUuid + "  received " + delivery.getEnvelope().toString());
        if (delivery.getBody() != null) {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            if (message.trim().length() > 0) {

                log.info(" [x] Received JOBS REQUEST '" + message + "' @ " + vmUuid);

                job = new Job().fromJSON(message);

                Status status = new Status(vmUuid, job.getUuid(), StatusState.RUNNING, CommonServerTestUtilities.JOB_MESSAGE_TYPE,
                        "job is starting", this.networkAddress);
                status.setStderr("");
                status.setStdout("");
                statusJSON = status.toJSON();

                log.info(" WORKER LAUNCHING JOB");

                // greedy acknowledge, it will be easier to deal with lost jobs than zombie workers in hostile OpenStack
                // environments
                log.info(vmUuid + " acknowledges " + delivery.getEnvelope().toString());
                jobChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                // we need to close the channel IMMEDIATELY to complete the ACK.
                jobChannel.close();
                // Close the connection object as well, or the main thread may not exit because of still-open-and-in-use resources.
                jobChannel.getConnection().close();

                workflowResult = new WorkflowResult();
                if (testMode) {
                    workflowResult.setWorkflowStdout("everything is awesome");
                    workflowResult.setExitCode(0);
                } else {
                    workflowResult = launchJob(statusJSON, job);
                }

                status = new Status(vmUuid, job.getUuid(),
                        workflowResult.getExitCode() == 0 ? StatusState.SUCCESS : StatusState.FAILED, CommonServerTestUtilities.JOB_MESSAGE_TYPE,
                        "job is finished", networkAddress);
                status.setStderr(workflowResult.getWorkflowStdErr());
                status.setStdout(workflowResult.getWorkflowStdout());
                statusJSON = status.toJSON();

                log.info(" WORKER FINISHING JOB");

                finishJob(statusJSON);
            } else {
                log.error(NO_MESSAGE_FROM_QUEUE_MESSAGE);
                throw new Exception("NO MESSAGE FROM JOB QUEUE!!!  MESSAGE SHOULD NOT BE NULL!!!");
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

            log.error(NO_MESSAGE_FROM_QUEUE_MESSAGE);
            throw new Exception("NO MESSAGE FROM JOB QUEUE!!!  MESSAGE SHOULD NOT BE NULL!!!");

        }
    }

    /**
     * This function will execute a workflow, based on the content of the Job object that is passed in.
     *
     * @param message
     *            - The message that will be published on the queue when the worker starts running the job.
     * @param job
     *            - The job contains information about what workflow to execute, and how.
     * @return The complete stdout and stderr from the workflow execution will be returned.
     */
    private WorkflowResult launchJob(String message, Job job) {
        WorkflowResult workflowResult = null;
        ExecutorService exService = Executors.newFixedThreadPool(2);
        WorkflowRunner workflowRunner = new WorkflowRunner();
        try {

            resultsChannel.basicPublish(this.resultsQueueName, this.resultsQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));
            resultsChannel.waitForConfirms();

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

            // dockstore-launcher has a separate config file
            File dockstoreLauncherConfig = new File("cwl-launcher.config");
            if (!dockstoreLauncherConfig.exists()){
                // if it doesn't exist, just create an empty one
                dockstoreLauncherConfig.createNewFile();
            }
            workflowRunner.setConfigFilePath(dockstoreLauncherConfig.getAbsolutePath());
            // write out descriptors from message
            final Path imageDescriptor = new File(System.getProperty("user.dir"), "image-descriptor."+job.getContainerImageDescriptorType()).toPath();
            final Path runDescriptor = new File(System.getProperty("user.dir"), "run-descriptor.json").toPath();
            FileUtils.writeStringToFile(imageDescriptor.toFile(), job.getContainerImageDescriptor(), StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(runDescriptor.toFile(), job.getContainerRuntimeDescriptor(), StandardCharsets.UTF_8);
            workflowRunner.setImageDescriptorPath(imageDescriptor.toFile().getAbsolutePath());
            workflowRunner.setRuntimeDescriptorPath(runDescriptor.toFile().getAbsolutePath());

            // write out extra files
            for(Map.Entry<String, Job.ExtraFile> entry : job.getExtraFiles().entrySet()){
                FileUtils.write(new File(entry.getKey()), entry.getValue().getContents(), StandardCharsets.UTF_8);
            }

            workflowRunner.setPreworkDelay(presleepMillis);
            workflowRunner.setPostworkDelay(postsleepMillis);
            // We will never actually do submit.get(), because the heartbeat should keep running until it is terminated by
            // exService.shutdownNow().
            Future<?> submit = exService.submit(heartbeat);
            Future<WorkflowResult> workflowResultFuture = exService.submit(workflowRunner);
            // make sure both are complete
            workflowResult = workflowResultFuture.get();
            // don't get the heartbeat if the workflow is complete already

            log.info("Docker execution result: " + workflowResult.getWorkflowStdout());
        } catch (IOException e) {
            // This could be caused by a problem writing the file, or publishing a message to the queue.
            log.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            log.error("Error executing workflow: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Workflow may have been interrupted: " + e.getMessage(), e);
        } finally {
            exService.shutdownNow();
        }

        return workflowResult;
    }

    /**
     * Get the IP address of this machine, preference is given to returning an IPv4 address, if there is one.
     *
     * @return An InetAddress object.
     * @throws SocketException thrown when unable to get access to network interface
     */
    protected InetAddress getFirstNonLoopbackAddress() throws SocketException {
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
