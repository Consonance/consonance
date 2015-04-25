package info.pancancer.arch3.coordinator;

import com.rabbitmq.client.*;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by boconnor on 15-04-18.
 *
 * This consumes the jobs and prepares messages for the VM and Job Queues.

 It then monitors the results queue to see when jobs fail or finish.

 Finally, for failed or finished workflows, it informats the VM about finished
 VMs that can be terminated.

 TODO:

 This needs to have a new thread that periodically checks on the DB table for Jobs to identify jobs that are lost/failed

 *
 */
public class Coordinator extends Base {

  private JSONObject settings = null;
  private Channel jobChannel = null;
  private Channel vmChannel = null;
  private Channel orderChannel = null;
  private Connection connection = null;
  private String queueName = null;
  private Utilities u = new Utilities();

  public static void main(String[] argv) throws Exception {

    OptionParser parser = new OptionParser();
    parser.accepts("config").withOptionalArg().ofType(String.class);
    OptionSet options = parser.parse(argv);

    String configFile = null;
    if (options.has("config")) {
      configFile = (String) options.valueOf("config");
    }

    // processes orders and turns them into requests for VMs/Containers (handled by ContainerProvisioner) and Jobs (handled by Worker)
    CoordinatorOrders c = new CoordinatorOrders(configFile);

    // this cleans up Jobs, currently this is just a DB update but in the future it's a Youxia call
    CleanupJobs t3 = new CleanupJobs(configFile);

  }

  public Coordinator (String configFile) {
    settings = u.parseConfig(configFile);
  }

}


class CoordinatorOrders {

  private JSONObject settings = null;
  private Channel jobChannel = null;
  private Channel vmChannel = null;
  private Channel orderChannel = null;
  private Connection connection = null;
  private String queueName = null;
  private Utilities u = new Utilities();

  private Inner inner;

  public CoordinatorOrders(String configFile) {
    inner = new Inner(configFile);
  }

  private class Inner extends Thread {

    private String configFile = null;
    private Logger log = LoggerFactory.getLogger(getClass());

    Inner(String config) {
      super(config);
      configFile = config;
      start();
    }

    public void run() {

      try {

        settings = u.parseConfig(configFile);

        queueName = (String) settings.get("rabbitMQQueueName");
        // read from
        orderChannel = u.setupQueue(settings, queueName + "_orders");
        // write to
        jobChannel = u.setupQueue(settings, queueName + "_jobs");  // TODO: actually this one needs to be built on demand with full info
        // write to
        vmChannel = u.setupQueue(settings, queueName + "_vms");
        // read from

        QueueingConsumer consumer = new QueueingConsumer(orderChannel);
        orderChannel.basicConsume(queueName + "_orders", true, consumer);

        // TODO: need threads that each read from orders and another that reads results
        while (true) {

          QueueingConsumer.Delivery delivery = consumer.nextDelivery();
          //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
          String message = new String(delivery.getBody());
          System.out.println(" [x] RECEIVED ORDER:\n'" + message + "'\n");

          // run the job
          Order order = new Order().fromJSON(message);

          String result = requestVm(order.getProvision().toJSON());
          String result2 = requestJob(order.getJob().toJSON());

        }

      } catch (IOException ex) {
        System.out.println(ex.toString());
        ex.printStackTrace();
      } catch (InterruptedException ex) {
        log.error(ex.toString());
      } catch (ShutdownSignalException ex) {
        log.error(ex.toString());
      } catch (ConsumerCancelledException ex) {
        log.error(ex.toString());
      }
    }


    private String requestVm(String message) {

      // TODO: should save information to persistant storage

      try {

        System.out.println(" + SENDING VM ORDER! " + queueName + "_vms");

        int messages = vmChannel.queueDeclarePassive(queueName + "_vms").getMessageCount();
        System.out.println("  + VM QUEUE SIZE: " + messages);

        vmChannel.basicPublish("", queueName + "_vms", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

        System.out.println(" + MESSAGE SENT!\n" + message + "\n");

      } catch (IOException ex) {
        log.error(ex.toString());
      }

      return null;

    }

    private String requestJob(String message) {

      StringBuffer result = new StringBuffer();

      try {

        // TODO: future feature...
        // So this is strange, why does the queue name have all this info in it?  It's
        // because we may have orders for the same workflow that actually need different resources
        //Channel vmchannel = u.setupQueue(settings, queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb);

        System.out.println(" + SENDING JOB ORDER! " + queueName + "_jobs");

        int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
        System.out.println("  + JOB QUEUE SIZE: " + messages);

        jobChannel.basicPublish("", queueName + "_jobs",
                MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

        JSONObject settings = u.parseConfig(this.configFile);
        PostgreSQL db = new PostgreSQL(settings);
        db.createJob(new Job().fromJSON(message));

        System.out.println(" + MESSAGE SENT!\n" + message + "\n");

      } catch (IOException ex) {
        log.error(ex.toString());
      }
      return result.toString();
    }

  }

}

/**
 * This dequeues the VM requests and stages them in the DB as pending so I can
 * keep a count of what's running/pending/finished.
 */
class CleanupJobs {

  private JSONObject settings = null;
  private Channel resultsChannel = null;
  private Channel vmChannel = null;
  private String queueName = null;
  private Utilities u = new Utilities();
  private QueueingConsumer resultsConsumer = null;

  private Inner inner;

  private class Inner extends Thread {

    private String configFile = null;

    Inner(String config) {
      super(config);
      configFile = config;
      start();
    }

    public void run() {
      try {

        settings = u.parseConfig(configFile);

        queueName = (String) settings.get("rabbitMQQueueName");

        // read from
        resultsChannel = u.setupMultiQueue(settings, queueName+"_results");
        // this declares a queue exchange where multiple consumers get the same message: https://www.rabbitmq.com/tutorials/tutorial-three-java.html
        String resultsQueue = resultsChannel.queueDeclare().getQueue();
        resultsChannel.queueBind(resultsQueue, queueName+"_results", "");
        resultsConsumer = new QueueingConsumer(resultsChannel);
        resultsChannel.basicConsume(resultsQueue, true, resultsConsumer);

        // writes to DB as well
        PostgreSQL db = new PostgreSQL(settings);


        // TODO: need threads that each read from orders and another that reads results
        while (true) {

          QueueingConsumer.Delivery delivery = resultsConsumer.nextDelivery();
          //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
          String message = new String(delivery.getBody());
          System.out.println(" [x] Received VM request '" + message + "'");

          // now parse it as JSONObj
          Status status = new Status().fromJSON(message);

          // now update that DB record to be exited
          // this is acutally finishing the VM and not the work
          if (status.getState().equals(u.SUCCESS) && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
            // this is where it reaps, the job status message also contains the UUID for the VM
            db.finishJob(status.getJobUuid());
          } else if ((status.getState().equals(u.RUNNING) || status.getState().equals(u.FAILED))
                  && Utilities.JOB_MESSAGE_TYPE.equals(status.getType())) {
            // this is where it reaps, the job status message also contains the UUID for the VM
            db.updateJob(status.getJobUuid(), status.getState());
          }

          // TODO: deal with other situations here like

          try {
            // pause
            Thread.sleep(5000);
          } catch (InterruptedException ex) {
            //log.error(ex.toString());
          }

        }

      } catch (IOException ex) {
        System.out.println(ex.toString());
        ex.printStackTrace();
      } catch (InterruptedException ex) {
        //log.error(ex.toString());
      } catch (ShutdownSignalException ex) {
        //log.error(ex.toString());
      } catch (ConsumerCancelledException ex) {
        //log.error(ex.toString());
      }
    }

  }

  public CleanupJobs(String configFile) {
    inner = new Inner(configFile);
  }
}
