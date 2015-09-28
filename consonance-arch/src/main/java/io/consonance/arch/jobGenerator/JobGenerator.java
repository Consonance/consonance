package io.consonance.arch.jobGenerator;

import com.google.common.base.Joiner;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import io.consonance.arch.Base;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.Order;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.common.Constants;
import io.consonance.arch.utils.IniFile;
import io.consonance.arch.utils.Utilities;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSpecBuilder;
import joptsimple.util.KeyValuePair;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Submits orders into the queue system.
 *
 * @author boconnor
 * @author dyuen
 */
public class JobGenerator extends Base {

    private final String flavour;
    private final String user;
    // variables
    private HierarchicalINIConfiguration settings = null;
    private Channel jchannel = null;
    private String queueName = null;
    private int currIterations = 0;

    private final OptionSpecBuilder forceSpec;

    public static void main(String[] argv) throws IOException {
        JobGenerator jg = new JobGenerator(argv);
        jg.log.info("MASTER FINISHED, EXITING!");
    }

    public JobGenerator(String[] argv) throws IOException {
        super();

        ArgumentAcceptingOptionSpec<String> iniDirSpec = super.parser
                .accepts("ini-dir", "schedule a batch of ini files from this directory").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<String> workflowNameSpec = super.parser.accepts("workflow-name", "track the name of workflows")
                .withOptionalArg().ofType(String.class).required();
        ArgumentAcceptingOptionSpec<String> workflowVersionSpec = super.parser.accepts("workflow-version", "track the version of workflows")
                .withOptionalArg().ofType(String.class).required();
        ArgumentAcceptingOptionSpec<String> workflowPathSpec = super.parser
                .accepts("workflow-path", "Schedule workflows at this path on the container host").withRequiredArg().ofType(String.class)
                .required();
        ArgumentAcceptingOptionSpec<Integer> totalJobSpec = super.parser
                .accepts("total-jobs", "Schedule a specific number of test workflows").requiredUnless(iniDirSpec, this.endlessSpec)
                .withRequiredArg().ofType(Integer.class).defaultsTo(Integer.MAX_VALUE);

        ArgumentAcceptingOptionSpec<String> userSpec = super.parser
                .accepts("user", "Designate a user for the jobs submitted in this batch").withRequiredArg().ofType(String.class)
                .defaultsTo("Player");
        ArgumentAcceptingOptionSpec<String> flavourSpec = super.parser
                .accepts("flavour", "Designate a specific flavour for the jobs submitted in this batch").withRequiredArg().ofType(String.class)
                .required();

        ArgumentAcceptingOptionSpec<KeyValuePair> extraFilesSpec = super.parser
                .accepts("extra-files", "Submit extra files that should exist on the worker when executing. This should be key values ").withRequiredArg().ofType(KeyValuePair.class)
                .withValuesSeparatedBy(',');

        this.forceSpec = super.parser.accepts("force", "Force job generation even if hashing is activated");

        parseOptions(argv);

        String iniDir = options.has(iniDirSpec) ? options.valueOf(iniDirSpec) : null;
        String workflowName = options.valueOf(workflowNameSpec);
        String workflowVersion = options.valueOf(workflowVersionSpec);
        String workflowPath = options.valueOf(workflowPathSpec);
        this.user = options.valueOf(userSpec);
        this.flavour = options.valueOf(flavourSpec);

        final Map<String, String> extraFiles = new HashMap<>();
        if (options.has(extraFilesSpec)){
            for (KeyValuePair keyValuePair : options.valuesOf(extraFilesSpec)) {
                extraFiles.put(keyValuePair.key, FileUtils.readFileToString(new File(keyValuePair.value)));
            }
        }

        // UTILS OBJECT
        settings = Utilities.parseConfig(configFile);

        // CONFIG
        queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
        log.info("queue name: " + queueName);
        try {
            // SETUP QUEUE
            this.jchannel = Utilities.setupQueue(settings, queueName + "_orders");
        } catch (TimeoutException ex) {
            throw new RuntimeException(ex);
        }
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
                generateAndQueueJob(iniDir + "/" + file, workflowName, workflowVersion, workflowPath, extraFiles);
            }
        } else if (options.has(endlessSpec) || options.has(totalJobSpec)) {
            // limit
            log.info("running in test mode");
            boolean endless = options.has(endlessSpec);
            int limit = options.valueOf(totalJobSpec);
            for (int i = 0; endless || i < limit; i++) {
                generateAndQueueJob(null, workflowName, workflowVersion, workflowPath, extraFiles);
            }
        }

        try {
            jchannel.getConnection().close(FIVE_SECOND_IN_MILLISECONDS);
        } catch (IOException ex) {
            log.error(ex.toString());
        }

    }

    private void generateAndQueueJob(String iniFile, String workflowName, String workflowVersion, String workflowPath, Map<String, String> extraFiles) {
        // keep track of the iterations
        currIterations++;
        log.info("\ngenerating new jobs, iteration " + currIterations + "\n");
        // TODO: this is fake, in a real program this is being read from JSON file or web service
        // check to see if new results are available and/or if the work queue is empty
        Order o = generateNewJob(iniFile, workflowName, workflowVersion, workflowPath, extraFiles);
        // enqueue new job
        if (o != null) {
            enqueueNewJobs(o.toJSON());
        }
        try {
            // pause
            Thread.sleep(ONE_SECOND_IN_MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    // PRIVATE

    // TODO: this will actually need to come from a file or web service
    private Order generateNewJob(String file, String workflowName, String workflowVersion, String workflowPath,
            Map<String, String> extraFiles) {

        Map<String, String> iniFileEntries;
        if (file != null) {
            // TODO: this will come from a web service or file
            iniFileEntries = parseIniFile(file);
            for (Entry<String, String> e : iniFileEntries.entrySet()) {
                log.info("key: " + e.getKey() + " value: " + e.getValue());
            }
        } else {
            iniFileEntries = new LinkedHashMap<>();
            iniFileEntries.put("param1", "bar");
            iniFileEntries.put("param2", "foo");
        }

        String hashStr;
        PostgreSQL db = new PostgreSQL(settings);

        Joiner.MapJoiner mapJoiner = Joiner.on(',').withKeyValueSeparator("=");
        String[] arr = this.settings.getStringArray(Constants.JOB_GENERATOR_FILTER_KEYS_IN_HASH);
        Map<String, String> filteredIniFileEntries = iniFileEntries;
        if (arr.length > 0) {
            System.out.println("Using ini hash filter set: " + Arrays.toString(arr));
            Set<String> keys = new HashSet<>();
            Map<String, String> filteredMap = new LinkedHashMap<>();
            keys.addAll(Arrays.asList(arr));
            for (Entry<String, String> entry : iniFileEntries.entrySet()) {
                if (keys.contains(entry.getKey())) {
                    filteredMap.put(entry.getKey(), entry.getValue());
                }
            }
            filteredIniFileEntries = filteredMap;
        }
        // don't use a real hashcode here, they can duplicate
        // just use the value of the filtered map
        hashStr = String.valueOf(mapJoiner.join(filteredIniFileEntries));

        if (this.settings.getBoolean(Constants.JOB_GENERATOR_CHECK_JOB_HASH, Boolean.FALSE)) {
            boolean runPreviously = db.previouslyRun(hashStr);
            if (runPreviously) {
                if (this.options.has(this.forceSpec)) {
                    System.out.println("Forcing scheduling, but would have skipped file (null if testing) due to hash: " + file);
                } else {
                    System.out.println("Skipping file (null if testing) due to hash: " + file);
                    return null;
                }
            }
        }

        int cores = DEFAULT_NUM_CORES;
        int memGb = DEFAULT_MEMORY;
        int storageGb = DEFAULT_DISKSPACE;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");

        Order newOrder = new Order();
        final Job job = new Job(workflowName, workflowVersion, workflowPath, hashStr, iniFileEntries);
        job.setFlavour(flavour);
        job.setEndUser(user);
        job.setExtraFiles(extraFiles);
        newOrder.setJob(job);
        newOrder.setProvision(new Provision(cores, memGb, storageGb, a));
        newOrder.getProvision().setJobUUID(newOrder.getJob().getUuid());
        return newOrder;
    }

    private Map<String, String> parseIniFile(String iniFile) {
        Map<String, String> iniHash = new LinkedHashMap<>();
        try {
            IniFile ini = new IniFile(iniFile);
            iniHash = ini.getEntries().get("no-section");
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
            jchannel.waitForConfirms();
        } catch (IOException | InterruptedException ex) {
            log.error(ex.toString());
        }

    }

}
