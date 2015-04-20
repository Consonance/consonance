package info.pancancer.arch3.worker;

import com.rabbitmq.client.*;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.utils.Utilities;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;

import java.io.IOException;

/**
 * Created by boconnor on 15-04-18.
 */
public class Worker extends Base {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel jobChannel = null;
    private Connection connection = null;
    private String queueName = null;
    private Utilities u = new Utilities();

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }
        Worker w = new Worker(configFile);

    }

    private Worker(String configFile) {

        try {

            settings = u.parseConfig(configFile);

            queueName = (String) settings.get("rabbitMQQueueName");

            // read from
            jobChannel = u.setupQueue(settings, queueName+"_jobs");

            // write to
            resultsChannel = u.setupQueue(settings, queueName+"_results");
            //resultsChannel.exchangeDeclare("results", "fanout");

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            jobChannel.basicConsume(queueName+"_jobs", true, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (true) {

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody());
                System.out.println(" [x] Received Jobs request '" + message + "'");

                // TODO: this will obviously get much more complicated when integrated with Youxia
                // launch VM
                String result = "{ \"Job-running-message\": {} }";
                launchJob(result);
                result = "{ \"Job-finished-message\": {} }";
                finishJob(result);

                try {
                    // pause
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    log.error(ex.toString());
                }

            }

        } catch (IOException ex) {
            System.out.println(ex.toString()); ex.printStackTrace();
        } catch (InterruptedException ex) {
            log.error(ex.toString());
        } catch (ShutdownSignalException ex) {
            log.error(ex.toString());
        } catch (ConsumerCancelledException ex) {
            log.error(ex.toString());
        }
    }

    // TOOD: obviously, this will need to launch something using Youxia in the future
    private void launchJob(String message) {
        try {
            resultsChannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
            //resultsChannel.basicPublish("results", "", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private void finishJob(String message) {
        try {
            resultsChannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
