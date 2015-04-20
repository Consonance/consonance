package info.pancancer.arch3.coordinator;

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
 *
 * This consumes the jobs and prepares messages for the VM and Job Queues.

 It then monitors the results queue to see when jobs fail or finish.

 Finally, for failed or finished workflows, it informats the VM about finished
 VMs that can be terminated.
 *
 */
public class Coordinator extends Base {

  private JSONObject settings = null;
  private Channel jobChannel = null;
  private Channel resultsChannel = null;
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
    if (options.has("config")) { configFile = (String)options.valueOf("config"); }
    Coordinator c = new Coordinator(configFile);

  }

  private Coordinator(String configFile) {

    try {

      settings = u.parseConfig(configFile);

      queueName = (String) settings.get("rabbitMQQueueName");
      // read from
      orderChannel = u.setupQueue(settings, queueName+"_orders");
      // write to
      jobChannel = u.setupQueue(settings, queueName+"_jobs");  // TODO: actually this one needs to be built on demand with full info
      // write to
      vmChannel = u.setupQueue(settings, queueName+"_vms");
      // read from
      resultsChannel = u.setupQueue(settings, queueName+"_results");

      QueueingConsumer consumer = new QueueingConsumer(orderChannel);
      orderChannel.basicConsume(queueName+"_orders", true, consumer);

      // TODO: need threads that each read from orders and another that reads results
      while (true) {

        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        //jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        String message = new String(delivery.getBody());
        System.out.println(" [x] Received job '" + message + "'");

        // run the job
        String result = requestVm(message);
        String result2 = requestJob(message);

        // TODO need to take action on results in the queue as well
        // read results back in results queue
        //readResults(result);

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

  private String requestVm(String message) {

    // TODO: should save information to persistant storage

    try {

      System.out.println("SENDING VM ORDER! "+queueName+"_vms");

      int messages = vmChannel.queueDeclarePassive(queueName + "_vms").getMessageCount();
      System.out.println("VM QUEUE SIZE: " + messages);

      vmChannel.basicPublish("", queueName + "_vms", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

      System.out.println(" + RESULTS SENT ! "+queueName+"_vms");

    } catch (IOException ex) {
      log.error(ex.toString());
    }

    return null;

  }

  private String requestJob(String message) {

    StringBuffer result = new StringBuffer();

    try {

      // first process the command
      JSONObject obj = u.parseJob(message);

      // - need to parse and enqueue into Job queue
      String workflowName = "";
      String workflowVersion = "";
      long cores = 0;
      long memGb = 0;
      long storageGb = 0;
      JSONObject job = (JSONObject) obj.get("job");
      workflowName = (String) job.get("workflow_name");
      workflowVersion = (String) job.get("workflow_version");
      JSONObject provision = (JSONObject) obj.get("provision");
      cores = (Long) provision.get("cores");
      memGb = (Long) provision.get("mem_gb");
      storageGb = (Long) provision.get("storage_gb");

      // TODO: future feature...
      // So this is strange, why does the queue name have all this info in it?  It's
      // because we may have orders for the same workflow that actually need different resources
      //Channel vmchannel = u.setupQueue(settings, queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb);

      System.out.println("SENDING JOB ORDER! "+queueName+"_jobs");

      int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
      System.out.println("JOB QUEUE SIZE: " + messages);

      jobChannel.basicPublish("", queueName+"_jobs",
              MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

      System.out.println("  + RESULTS SENT!");

    } catch (IOException ex) {
      log.error(ex.toString());
    }
    return result.toString();
  }

  // TODO: probably not needed
  private void reportResult(String result) {
    try {

      System.out.println("SENDING RESULTS BACK! "+queueName+"_results");

      resultsChannel.basicPublish("", queueName + "_results", MessageProperties.PERSISTENT_TEXT_PLAIN, result.getBytes());

      System.out.println("  + RESULTS SENT BACK! "+queueName+"_results");


    } catch (IOException ex) {
      log.error(ex.toString());
    }
  }

}
