package info.pancancer.arch3.containerProvisioner;

import info.pancancer.arch3.Base;
import com.rabbitmq.client.*;
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
public class ContainerProvisioner extends Base {

    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel vmChannel = null;
    private Connection connection = null;
    private String queueName = null;
    private Utilities u = new Utilities();

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }
        ContainerProvisioner c = new ContainerProvisioner(configFile);

    }

    private ContainerProvisioner(String configFile) {

        try {

            settings = u.parseConfig(configFile);

            queueName = (String) settings.get("rabbitMQQueueName");

            // read from
            vmChannel = u.setupQueue(settings, queueName+"_vms");

            // write to
            resultsChannel = u.setupMultiQueue(settings, queueName+"_results");

            QueueingConsumer consumer = new QueueingConsumer(vmChannel);
            vmChannel.basicConsume(queueName+"_vms", true, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (true) {

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message = new String(delivery.getBody());
                System.out.println(" [x] Received VM request '" + message + "'");



                /*
                So the logic for this is this tool dequeues requests and stores an order in the storage system.
                That order's initial state is queued.  It then does a query to find the count of queued VMs
                and then sees if that is > the max number of VMs to launch.  if
                 */
                // TODO: this will obviously get much more complicated when integrated with Youxia launch VM
                String result = "{ \"VM-launched-message\": {} }";
                launchVM(result);

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
    private void launchVM(String message) {
        try {
            //resultsChannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
            resultsChannel.basicPublish(queueName+"_results", "", null, message.getBytes());

            /*
            As a temporary test this code will launch a worker thread which will run for one job then
            exit.  This will allow us to simulate
             */

        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
