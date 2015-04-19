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
        String result = requestVm(message);
        String result = runJob(message);

        // save results back in results queue
        //reportResult(result);

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

    try {

      Channel vmchannel = u.setupQueue(settings, queueName+"_vm_requests");

      System.out.println("SENDING VM ORDER! "+queueName+"_vm_requests");

      vmchannel.basicPublish("", queueName + "_vm_requests", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

      System.out.println("  + RESULTS SENT BACK! "+queueName+"_vm_requests");

    } catch (IOException ex) {
      log.error(ex.toString());
    }

    return null;

  }

  private String runJob(String job) {
    StringBuffer result = new StringBuffer();
    try {

      // first process the command
      JSONObject obj = u.parseJob(job);

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

      Channel vmchannel = u.setupQueue(settings, queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb);

      System.out.println("SENDING JOB ORDER! "+queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb);

      vmchannel.basicPublish("", queueName+"_job_requests_"+workflowName+"_"+workflowVersion+"_"+cores+"_"+memGb+"_"+storageGb,
              MessageProperties.PERSISTENT_TEXT_PLAIN, job.getBytes());

      System.out.println("  + RESULTS SENT BACK!");



      // TODO: this will need to be moved into the Util and Worker classes
      /*String cmd = (String) obj.get("command");
      System.out.println("JOB: "+cmd);

      Process p = new ProcessBuilder(cmd.split("\\s+")).start();
      BufferedReader reader = new BufferedReader(new InputStreamReader((p.getInputStream())));
      String currLine;
      while((currLine = reader.readLine()) != null) {
        result.append(currLine + "\n");
      }*/
    } catch (IOException ex) {
      log.error(ex.toString());
    }
    return result.toString();
  }

  private void reportResult(String result) {
    try {

      System.out.println("SENDING RESULTS BACK! "+queueName+"_results");

      this.rchannel.basicPublish("", queueName+"_results", MessageProperties.PERSISTENT_TEXT_PLAIN, result.getBytes());

      System.out.println("  + RESULTS SENT BACK! "+queueName+"_results");


    } catch (IOException ex) {
      Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

}
