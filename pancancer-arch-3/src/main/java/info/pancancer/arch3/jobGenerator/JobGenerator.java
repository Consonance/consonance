package info.pancancer.arch3.jobGenerator;

import com.google.common.base.Joiner;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.IniFile;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import joptsimple.ArgumentAcceptingOptionSpec;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Created by boconnor on 15-04-18.
 *
 * takes a --config option pointed to a config .json file
 */
public class JobGenerator extends Base {

    // variables
    private HierarchicalINIConfiguration settings = null;
    private Channel jchannel = null;
    private String queueName = null;
    private int currIterations = 0;

    private final ArgumentAcceptingOptionSpec<String> iniDirSpec;
    private final ArgumentAcceptingOptionSpec<String> workflowNameSpec;
    private final ArgumentAcceptingOptionSpec<String> workflowVersionSpec;
    private final ArgumentAcceptingOptionSpec<String> workflowPathSpec;
    private final ArgumentAcceptingOptionSpec<Integer> totalJobSpec;

    public static void main(String[] argv) throws IOException {
        JobGenerator jg = new JobGenerator(argv);
        jg.log.info("MASTER FINISHED, EXITING!");
    }

    public JobGenerator(String[] argv) throws IOException {
        super();
        this.iniDirSpec = super.parser.accepts("ini-dir").withOptionalArg().ofType(String.class);
        this.workflowNameSpec = super.parser.accepts("workflow-name").withOptionalArg().ofType(String.class).defaultsTo("DEWrapper");
        this.workflowVersionSpec = super.parser.accepts("workflow-version").withOptionalArg().ofType(String.class).defaultsTo("1.0.0");
        this.workflowPathSpec = super.parser.accepts("workflow-path").withOptionalArg().ofType(String.class)
                .defaultsTo("/workflow/Workflow_Bundle_DEWrapperWorkflow_1.0.0_SeqWare_1.1.0");
        this.totalJobSpec = super.parser.accepts("total-jobs").requiredUnless(iniDirSpec, this.endlessSpec).withRequiredArg()
                .ofType(Integer.class).defaultsTo(Integer.MAX_VALUE);

        parseOptions(argv);

        String iniDir = options.has(iniDirSpec) ? options.valueOf(iniDirSpec) : null;
        String workflowName = options.valueOf(workflowNameSpec);
        String workflowVersion = options.valueOf(workflowVersionSpec);
        String workflowPath = options.valueOf(workflowPathSpec);

        // UTILS OBJECT
        settings = Utilities.parseConfig(configFile);

        // CONFIG
        queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
        log.info("QUEUE NAME: " + queueName);

        // SETUP QUEUE
        this.jchannel = Utilities.setupQueue(settings, queueName + "_orders");

        if (options.has(iniDirSpec)) {
            // read an array of files
            log.info("scanning: " + iniDir);
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setIncludes(new String[] { "**/*.ini" });
            scanner.setBasedir(iniDir);
            scanner.setCaseSensitive(false);
            scanner.scan();
            String[] files = scanner.getIncludedFiles();

            // LOOP, ADDING JOBS EVERY FEW MINUTES
            for (String file : files) {
                generateAndQueueJob(iniDir + "/" + file, workflowName, workflowVersion, workflowPath);
            }
        } else if (options.has(endlessSpec) || options.has(totalJobSpec)) {
            // limit
            log.info("running in test mode");
            boolean endless = options.has(endlessSpec);
            int limit = options.valueOf(totalJobSpec);
            for (int i = 0; endless || i < limit; i++) {
                generateAndQueueJob(null, workflowName, workflowVersion, workflowPath);
            }
        }

        try {

            jchannel.getConnection().close(FIVE_SECOND_IN_MILLISECONDS);

        } catch (IOException ex) {
            log.error(ex.toString());
        }

    }

    private void generateAndQueueJob(String iniFile, String workflowName, String workflowVersion, String workflowPath) {
        // keep track of the iterations
        currIterations++;
        log.info("\nGENERATING NEW JOBS, iteration " + currIterations + "\n");
        // TODO: this is fake, in a real program this is being read from JSONL file or web service
        // check to see if new results are available and/or if the work queue is empty
        Order o = generateNewJob(iniFile, workflowName, workflowVersion, workflowPath);
        // enqueue new job
        enqueueNewJobs(o.toJSON());
        try {
            // pause
            Thread.sleep(ONE_SECOND_IN_MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    // PRIVATE

    // TODO: this will actually need to come from a file or web service
    private Order generateNewJob(String file, String workflowName, String workflowVersion, String workflowPath) {

        Map<String, String> hm;
        if (file != null) {
            // TODO: this will come from a web service or file
            hm = parseIniFile(file);
            for (Entry<String, String> e : hm.entrySet()) {
                log.info("KEY: " + e.getKey() + " VALUE: " + e.getValue());
            }
        } else {
            hm = new HashMap<>();
            hm.put("param1", "bar");
            hm.put("param2", "foo");
        }

        Joiner.MapJoiner mapJoiner = Joiner.on(',').withKeyValueSeparator("=");
        String hashStr = String.valueOf(mapJoiner.join(hm).hashCode());

        int cores = DEFAULT_NUM_CORES;
        int memGb = DEFAULT_MEMORY;
        int storageGb = DEFAULT_DISKSPACE;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");

        Order newOrder = new Order();
        newOrder.setJob(new Job(workflowName, workflowVersion, workflowPath, hashStr, hm));
        newOrder.setProvision(new Provision(cores, memGb, storageGb, a));
        newOrder.getProvision().setJobUUID(newOrder.getJob().getUuid());

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
