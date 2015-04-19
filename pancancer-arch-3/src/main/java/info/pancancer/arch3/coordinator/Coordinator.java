package info.pancancer.arch3.coordinator;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.json.simple.JSONObject;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.utils.Utilities;

/**
 * Created by boconnor on 15-04-18.
 */
public class Coordinator extends Base {

  private JSONObject settings = null;
  private Channel jchannel = null;
  private Channel rchannel = null;
  private Connection connection = null;
  private String queueName = null;
  private Utilities u = new Utilities();

  public static void main(String[] argv) throws Exception {

    OptionParser parser = new OptionParser();
    parser.accepts("config").withOptionalArg().ofType(String.class);
    OptionSet options = parser.parse(argv);

    String configFile = null;
    if (options.has("config")) { configFile = (String)options.valueOf("config"); }
    Coordinator c = new Coordinator(configFile);

  }

  private Coordinator(String configFile) {

    try {

      settings = u.parseConfig(configFile);
      queueName = (String) settings.get("rabbitMQQueueName");
      this.jchannel = u.setupQueue(settings, queueName+"_jobs");
      this.rchannel = u.setupQueue(settings, queueName+"_results");

      QueueingConsumer consumer = new QueueingConsumer(jchannel);
      jchannel.basicConsume(queueName+"_jobs", true, consumer);

      while (true) {

        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        String message = new String(delivery.getBody());
        System.out.println(" [x] Received job '" + message + "'");

        // run the job
        String result = runJob(message);

        // save results back in results queue
        reportResult(result);

        try {
          // pause
          Thread.sleep(5000);
        } catch (InterruptedException ex) {
          log.error(ex.toString());
        }

      }

    } catch (IOException ex) {
      log.error(ex.toString());
    } catch (InterruptedException ex) {
      log.error(ex.toString());
    } catch (ShutdownSignalException ex) {
      log.error(ex.toString());
    } catch (ConsumerCancelledException ex) {
      log.error(ex.toString());
    }
  }

}
