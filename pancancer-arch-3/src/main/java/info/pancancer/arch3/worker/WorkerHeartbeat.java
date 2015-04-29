package info.pancancer.arch3.worker;

import java.io.IOException;

import com.rabbitmq.client.Channel;

public class WorkerHeartbeat implements Runnable {

    private Channel reportingChannel;
    private String queueName;

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

    @Override
    public void run() {
        byte[] body = null;
        try {
            reportingChannel.basicPublish("", queueName+"_results", null, body);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
