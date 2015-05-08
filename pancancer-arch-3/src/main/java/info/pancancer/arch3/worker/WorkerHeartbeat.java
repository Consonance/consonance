package info.pancancer.arch3.worker;

import info.pancancer.arch3.Base;

import java.io.IOException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.Channel;

public class WorkerHeartbeat implements Runnable {

    private static final CharsetEncoder ENCODER = StandardCharsets.UTF_8.newEncoder();
    private Channel reportingChannel;
    private String queueName;
    private double secondsDelay = 2.0;
    private String messageBody;

    public volatile boolean stop = false;

    @Override
    public void run() {
        //while (!Thread.currentThread().isInterrupted()) {
        System.out.println("starting heartbeat thread, will send heartbeat message ever "+secondsDelay + " seconds.");
        while (!Thread.currentThread().interrupted()) {
            byte[] body = this.getMessageBody().getBytes(ENCODER.charset());
            try {
                System.out.println("Sending heartbeat message to "+queueName+", with body: "+this.getMessageBody());
                reportingChannel.basicPublish(queueName, queueName , null, body);
                Thread.sleep((long) (Base.ONE_SECOND_IN_MILLISECONDS));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Heartbeat shutting down.");
            }
        }
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

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }


}
