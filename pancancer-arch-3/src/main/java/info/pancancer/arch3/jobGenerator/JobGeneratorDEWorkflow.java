package info.pancancer.arch3.jobGenerator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.utils.IniFile;
import info.pancancer.arch3.utils.Utilities;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.tools.ant.DirectoryScanner;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by boconnor on 15-04-18.
 *
 * takes a --config option pointed to a config .json file
 */
public class JobGeneratorDEWorkflow extends Base {

    // variables
    private String outputFile = null;
    private JSONObject settings = null;
    private Channel jchannel = null;
    private Connection connection = null;
    private String queueName = null;
    private QueueingConsumer rconsumer = null;
    private Date start = new Date();
    private int currIterations = 0;
    private int overallIterationsMax = 0;
    private int overallRuntimeMaxHours = 0;

    public static void main(String [] args)
    {
        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        parser.accepts("ini-dir").withOptionalArg().ofType(String.class);
        parser.accepts("workflow-name").withOptionalArg().ofType(String.class);
        parser.accepts("workflow-version").withOptionalArg().ofType(String.class);
        parser.accepts("workflow-path").withOptionalArg().ofType(String.class);

        OptionSet options = parser.parse(args);

        String configFile = null;
        String iniDir = null;
        String workflowName = "DEWrapper";
        String workflowVersion = "1.0.0";
        String workflowPath = "/workflow/Workflow_Bundle_DEWrapperWorkflow_1.0.0_SeqWare_1.1.0";
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }
        if (options.has("ini-dir")) { iniDir = (String)options.valueOf("ini-dir"); }
        if (options.has("workflow-name")) { workflowName = (String)options.valueOf("workflow-name"); }
        if (options.has("workflow-version")) { workflowVersion = (String)options.valueOf("workflow-version"); }
        if (options.has("workflow-path")) { workflowPath = (String)options.valueOf("workflow-path"); }

        JobGeneratorDEWorkflow jg = new JobGeneratorDEWorkflow(configFile, iniDir, workflowName, workflowVersion, workflowPath);

        jg.log.info("MASTER FINISHED, EXITING!");
    }

    public JobGeneratorDEWorkflow(String configFile, String iniDir, String workflowName, String workflowVersion, String workflowPath) {


        // UTILS OBJECT
        Utilities u = new Utilities();
        settings = u.parseConfig(configFile);

        // CONFIG
        queueName = (String) settings.get("rabbitMQQueueName");
        System.out.println("QUEUE NAME: "+queueName);

        // SETUP QUEUE
        this.jchannel = u.setupQueue(settings, queueName + "_orders");

        // read an array of files
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{"**/*.ini"});
        scanner.setBasedir(iniDir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        // LOOP, ADDING JOBS EVERY FEW MINUTES
        for (String file : files) {

            // keep track of the iterations
            currIterations++;

            System.out.println("\nGENERATING NEW JOBS\n");
            // TODO: this is fake, in a real program this is being read from JSONL file or web service
            // check to see if new results are available and/or if the work queue is empty
            Order o = generateNewJob(iniDir+"/"+file, workflowName, workflowVersion, workflowPath, u);

            // enqueue new job
            enqueueNewJobs(o.toJSON());


            try {
                // pause
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.error(ex.toString());
            }

        }

        try {

            jchannel.getConnection().close(5000);

        } catch (IOException ex) {
            log.error(ex.toString());
        }


    }

    // PRIVATE

    // TODO: this will actually need to come from a file or web service
    private Order generateNewJob(String file, String workflowName, String workflowVersion, String workflowPath, Utilities u) {

        // TODO: will need to make this from parameters in INI
        String uuid = UUID.randomUUID().toString().toLowerCase();
        String hashStr = u.digest(uuid);

        // TODO: this will come from a web service or file
        HashMap<String, String> hm = parseIniFile(file);

        for (String key : hm.keySet()) {
            System.out.println("KEY: "+key+" VALUE: "+hm.get(key));
        }

        // if this is the donor then use it for the hashStr
        if (hm.containsKey("donor_id") && hm.containsKey("project_code")) { hashStr = hm.get("project_code") + "::" + hm.get("donor_id"); }

        int cores = 8;
        int memGb = 128;
        int storageGb = 1024;
        ArrayList<String> a = new ArrayList<String>();
        a.add("ansible_playbook_path");

        Order newOrder = new Order(workflowName, workflowVersion, workflowPath, hashStr, hm, cores, memGb, storageGb, a);

        return(newOrder);


    }

    private HashMap<String, String> parseIniFile(String iniFile) {

        HashMap<String, String> iniHash = new HashMap<String, String>();

        try {
            IniFile ini = new IniFile(iniFile);
            iniHash = (HashMap<String, String>) ini.getEntries().get("no-section");

        } catch (IOException e) {
            log.error(e.toString());
        }

        return (iniHash);
    }

    private boolean exceededTimeOrJobs() {

        // FIXME: hardcoded for testing
        return(false);

    }

    private void enqueueNewJobs(String job) {

        try {
            System.out.println("\nSENDING JOB:\n '" + job + "'\n" + this.jchannel+" \n");

            this.jchannel.basicPublish("", queueName + "_orders", MessageProperties.PERSISTENT_TEXT_PLAIN, job.getBytes());
        } catch (IOException ex) {
            log.error(ex.toString());
        }

    }

}
