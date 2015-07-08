package info.pancancer.arch3.worker;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.utils.Utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will send a "heartbeat" message. How often it is sent can be configured via setSecondsDelay. The default delay is 2 seconds.
 * The destination queue for the messages, and the body of the messages can also be configured via setter methods.
 *
 * @author sshorser
 *
 */
public class WorkerHeartbeat implements Runnable {

    private String queueName;
    private double secondsDelay = DEFAULT_DELAY;
    public static final double DEFAULT_DELAY = 30.0;
    private final int stdoutSnipSize = DEFAULT_SNIP_SIZE;
    public static final int DEFAULT_SNIP_SIZE = 10;
    private WorkflowRunner statusSource;
    private String networkID;
    private String vmUuid;
    private String jobUuid;
    private HierarchicalINIConfiguration settings;

    protected static final Logger LOG = LoggerFactory.getLogger(WorkerHeartbeat.class);

    @Override
    public void run() {
        LOG.info("starting heartbeat thread, will send heartbeat message ever " + secondsDelay + " seconds.");
        Channel reportingChannel = Utilities.setupExchange(settings, this.queueName);
        while (!Thread.currentThread().interrupted()) {

            // byte[] stdOut = this.getMessageBody().getBytes(StandardCharsets.UTF_8);
            try {
                try {
                    Status heartbeatStatus = new Status();
                    heartbeatStatus.setJobUuid(this.jobUuid);
                    heartbeatStatus.setMessage("job is running; IP address: " + networkID);
                    heartbeatStatus.setState(StatusState.RUNNING);
                    heartbeatStatus.setType(Utilities.JOB_MESSAGE_TYPE);
                    heartbeatStatus.setVmUuid(this.vmUuid);
                    heartbeatStatus.setIpAddress(networkID);

                    // String stdOut = this.statusSource.getStdOut();
                    Lock lock = new ReentrantLock();
                    lock.lock();
                    String stdOut = this.statusSource.getStdOut(stdoutSnipSize);
                    String stdErr = this.statusSource.getStdErr();
                    lock.unlock();
                    heartbeatStatus.setStdout(stdOut);
                    heartbeatStatus.setStderr(stdErr);
                    String heartBeatMessage = heartbeatStatus.toJSON();
                    LOG.debug("Sending heartbeat message to " + queueName + ", with body: " + heartBeatMessage);
                    reportingChannel.basicPublish(queueName, queueName, MessageProperties.PERSISTENT_TEXT_PLAIN,
                            heartBeatMessage.getBytes(StandardCharsets.UTF_8));
                    reportingChannel.waitForConfirms();

                    Thread.sleep((long) (Base.ONE_SECOND_IN_MILLISECONDS));
                } catch (IOException | AlreadyClosedException e) {
                    LOG.error("IOException caught! Message may not have been published. Exception is: " + e.getMessage(), e);
                    // retry after a minute, do not die simply because the launcher is unavailable, it may come back
                    Thread.sleep(Base.ONE_MINUTE_IN_MILLISECONDS);
                }
            } catch (InterruptedException e) {
                LOG.info("Heartbeat shutting down.");
                if (reportingChannel.getConnection().isOpen()) {
                    try {
                        reportingChannel.getConnection().close();
                    } catch (IOException e1) {
                        LOG.error("Error closing reportingChannel connection: " + e1.getMessage(), e1);
                    }
                }
                if (reportingChannel.isOpen()) {
                    try {
                        reportingChannel.close();
                    } catch (IOException e1) {
                        LOG.error("Error (IOException) closing reportingChannel: " + e1.getMessage(), e1);
                    } catch (TimeoutException e1) {
                        LOG.error("Error (TimeoutException) closing reportingChannel: " + e1.getMessage(), e1);
                    }
                }
                LOG.debug("reporting channel open: "+reportingChannel.isOpen());
                LOG.debug("reporting channel connection open: "+reportingChannel.getConnection().isOpen());

                Thread.currentThread().interrupt();
            }
        }
    }

    public void setStatusSource(WorkflowRunner runner) {
        this.statusSource = runner;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public double getSecondsDelay() {
        return secondsDelay;
    }

    public void setSecondsDelay(double secondsDelay) {
        this.secondsDelay = secondsDelay;
    }

    public String getNetworkID() {
        return networkID;
    }

    public void setNetworkID(String networkID) {
        this.networkID = networkID;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public WorkflowRunner getStatusSource() {
        return statusSource;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    /**
     * @return the settings
     */
    public HierarchicalINIConfiguration getSettings() {
        return settings;
    }

    /**
     * @param settings
     *            the settings to set
     */
    public void setSettings(HierarchicalINIConfiguration settings) {
        this.settings = settings;
    }

}
