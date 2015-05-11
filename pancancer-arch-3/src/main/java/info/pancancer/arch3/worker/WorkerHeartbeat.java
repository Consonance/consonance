package info.pancancer.arch3.worker;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.beans.StatusState;
import info.pancancer.arch3.utils.Utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.rabbitmq.client.Channel;

/**
 * This class will send a "heartbeat" message. How often it is sent can be configured via setSecondsDelay. The default delay is 2 seconds.
 * The destination queue for the messages, and the body of the messages can also be configured via setter methods.
 * 
 * @author sshorser
 *
 */
public class WorkerHeartbeat implements Runnable {

    private Channel reportingChannel;
    private String queueName;
    private double secondsDelay = 2.0;
    private WorkflowRunner statusSource;
    private String networkID;
    private String vmUuid;
    private String jobUuid;

    @Override
    public void run() {
        System.out.println("starting heartbeat thread, will send heartbeat message ever " + secondsDelay + " seconds.");
        while (!Thread.currentThread().interrupted()) {

            // byte[] stdOut = this.getMessageBody().getBytes(StandardCharsets.UTF_8);
            try {
                Status heartbeatStatus = new Status();
                heartbeatStatus.setJobUuid(this.jobUuid);
                heartbeatStatus.setMessage("job is running; IP address: " + networkID);
                heartbeatStatus.setState(StatusState.RUNNING);
                heartbeatStatus.setType(Utilities.JOB_MESSAGE_TYPE);
                heartbeatStatus.setVmUuid(this.vmUuid);

                //String stdOut = this.statusSource.getStdOut();
                Lock lock = new ReentrantLock();
                lock.lock();
                String stdOut = this.statusSource.getStdOut(2);
                String stdErr = this.statusSource.getStdErr();
                lock.unlock();
                heartbeatStatus.setStdout(stdOut);
                heartbeatStatus.setStderr(stdErr);
                String heartBeatMessage = heartbeatStatus.toJSON();
                System.out.println("Sending heartbeat message to " + queueName + ", with body: " + heartBeatMessage);
                reportingChannel.basicPublish(queueName, queueName, null, heartBeatMessage.getBytes(StandardCharsets.UTF_8));
                Thread.sleep((long) (Base.ONE_SECOND_IN_MILLISECONDS));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Heartbeat shutting down.");
            }
        }
    }

    public void setStatusSource(WorkflowRunner runner) {
        this.statusSource = runner;
    }

    public Channel getReportingChannel() {
        return reportingChannel;
    }

    public void setReportingChannel(Channel reportingChannel) {
        this.reportingChannel = reportingChannel;
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

}
