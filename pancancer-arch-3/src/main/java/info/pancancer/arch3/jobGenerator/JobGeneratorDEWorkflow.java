package info.pancancer.arch3.jobGenerator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.utils.IniFile;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import joptsimple.ArgumentAcceptingOptionSpec;
import org.apache.tools.ant.DirectoryScanner;
import org.json.simple.JSONObject;

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

    private final ArgumentAcceptingOptionSpec<String> iniDirSpec;
    private final ArgumentAcceptingOptionSpec<String> workflowNameSpec;
    private final ArgumentAcceptingOptionSpec<String> workflowVersionSpec;
    private final ArgumentAcceptingOptionSpec<String> workflowPathSpec;

    public static void main(String[] argv) throws IOException {
        JobGeneratorDEWorkflow jg = new JobGeneratorDEWorkflow(argv);
        jg.log.info("MASTER FINISHED, EXITING!");
    }

    public JobGeneratorDEWorkflow(String[] argv) throws IOException {
        super();
        this.iniDirSpec = super.parser.accepts("ini-dir").withOptionalArg().ofType(String.class).defaultsTo("null");
        this.workflowNameSpec = super.parser.accepts("workflow-name").withOptionalArg().ofType(String.class).defaultsTo("DEWrapper");
        this.workflowVersionSpec = super.parser.accepts("workflow-version").withOptionalArg().ofType(String.class).defaultsTo("1.0.0");
        this.workflowPathSpec = super.parser.accepts("workflow-path").withOptionalArg().ofType(String.class)
                .defaultsTo("/workflow/Workflow_Bundle_DEWrapperWorkflow_1.0.0_SeqWare_1.1.0");
        parseOptions(argv);

        String iniDir = options.valueOf(iniDirSpec);
        String workflowName = options.valueOf(workflowNameSpec);
        String workflowVersion = options.valueOf(workflowVersionSpec);
        String workflowPath = options.valueOf(workflowPathSpec);

        // UTILS OBJECT
        Utilities u = new Utilities();
        settings = Utilities.parseConfig(configFile);

        // CONFIG
        queueName = (String) settings.get("rabbitMQQueueName");
        log.info("QUEUE NAME: " + queueName);

        // SETUP QUEUE
        this.jchannel = Utilities.setupQueue(settings, queueName + "_orders");

        // read an array of files
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[] { "**/*.ini" });
        scanner.setBasedir(iniDir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        // LOOP, ADDING JOBS EVERY FEW MINUTES
        for (String file : files) {

            // keep track of the iterations
            currIterations++;

            log.info("\nGENERATING NEW JOBS\n");
            // TODO: this is fake, in a real program this is being read from JSONL file or web service
            // check to see if new results are available and/or if the work queue is empty
            Order o = generateNewJob(iniDir + "/" + file, workflowName, workflowVersion, workflowPath, u);

            // enqueue new job
            enqueueNewJobs(o.toJSON());

            try {
                // pause
                Thread.sleep(ONE_SECOND_IN_MILLISECONDS);
            } catch (InterruptedException ex) {
                log.error(ex.getMessage(), ex);
            }

        }

        try {

            jchannel.getConnection().close(FIVE_SECOND_IN_MILLISECONDS);

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
            log.info("KEY: " + key + " VALUE: " + hm.get(key));
        }

        // if this is the donor then use it for the hashStr
        if (hm.containsKey("donor_id") && hm.containsKey("project_code")) {
            hashStr = hm.get("project_code") + "::" + hm.get("donor_id");
        }

        int cores = DEFAULT_NUM_CORES;
        int memGb = DEFAULT_MEMORY;
        int storageGb = DEFAULT_DISKSPACE;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");

        Order newOrder = new Order();
        newOrder.setJob(new Job(workflowName, workflowVersion, workflowPath, hashStr, hm));
        newOrder.setProvision(new Provision(cores, memGb, storageGb, a));

        return newOrder;

    }

    private HashMap<String, String> parseIniFile(String iniFile) {

        HashMap<String, String> iniHash = new HashMap<>();

        try {
            IniFile ini = new IniFile(iniFile);
            iniHash = (HashMap<String, String>) ini.getEntries().get("no-section");

        } catch (IOException e) {
            log.error(e.toString());
        }

        return iniHash;
    }

    private boolean exceededTimeOrJobs() {

        // FIXME: hardcoded for testing
        return false;

    }

    private void enqueueNewJobs(String job) {

        try {
            log.info("\nSENDING JOB:\n '" + job + "'\n" + this.jchannel + " \n");

            this.jchannel.basicPublish("", queueName + "_orders", MessageProperties.PERSISTENT_TEXT_PLAIN,
                    job.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.error(ex.toString());
        }

    }

}
