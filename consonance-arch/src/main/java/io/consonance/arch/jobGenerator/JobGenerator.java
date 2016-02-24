/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.consonance.arch.jobGenerator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import io.consonance.arch.Base;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.Order;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.utils.CommonServerTestUtilities;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Submits orders into the queue system.
 *
 * @deprecated needs to be hooked up the web service for Consonance 2
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
        this.user = options.valueOf(userSpec);
        this.flavour = options.valueOf(flavourSpec);

        final Map<String, Job.ExtraFile> extraFiles = new HashMap<>();
        if (options.has(extraFilesSpec)){
            for (KeyValuePair keyValuePair : options.valuesOf(extraFilesSpec)) {
                Job.ExtraFile file = new Job.ExtraFile(FileUtils.readFileToString(new File(keyValuePair.value)), false);
                extraFiles.put(keyValuePair.key, file);
            }
        }

        // UTILS OBJECT
        settings = CommonTestUtilities.parseConfig(configFile);

        // CONFIG
        queueName = settings.getString(Constants.RABBIT_QUEUE_NAME);
        log.info("queue name: " + queueName);
        try {
            // SETUP QUEUE
            this.jchannel = CommonServerTestUtilities.setupQueue(settings, queueName + "_orders");
        } catch (InterruptedException ex) {
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
                generateAndQueueJob(iniDir + "/" + file, extraFiles);
            }
        } else if (options.has(endlessSpec) || options.has(totalJobSpec)) {
            // limit
            log.info("running in test mode");
            boolean endless = options.has(endlessSpec);
            int limit = options.valueOf(totalJobSpec);
            for (int i = 0; endless || i < limit; i++) {
                generateAndQueueJob(null, extraFiles);
            }
        }

        try {
            jchannel.getConnection().close(FIVE_SECOND_IN_MILLISECONDS);
        } catch (IOException ex) {
            log.error(ex.toString());
        }

    }

    private void generateAndQueueJob(String iniFile, Map<String, Job.ExtraFile> extraFiles) {
        // keep track of the iterations
        currIterations++;
        log.info("\ngenerating new jobs, iteration " + currIterations + "\n");
        // TODO: this is fake, in a real program this is being read from JSON file or web service
        // check to see if new results are available and/or if the work queue is empty
        Order o = generateNewJob(iniFile, extraFiles);
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
    private Order generateNewJob(String file, Map<String, Job.ExtraFile> extraFiles) {

        //String hashStr;
        //PostgreSQL db = new PostgreSQL(settings);

        //Joiner.MapJoiner mapJoiner = Joiner.on(',').withKeyValueSeparator("=");
        //String[] arr = this.settings.getStringArray(Constants.JOB_GENERATOR_FILTER_KEYS_IN_HASH);
        // TODO: we no longer use ini files, so hash generation will need to be re-thought

        if (this.settings.getBoolean(Constants.JOB_GENERATOR_CHECK_JOB_HASH, Boolean.FALSE)) {
            boolean runPreviously = false;//db.previouslyRun(hashStr);
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

        // TODO: this is totally broken until we update to use CWL
        Order newOrder = new Order();
        final Job job = new Job(""/**hashStr*/);
        job.setFlavour(flavour);
        job.setEndUser(user);
        job.setExtraFiles(extraFiles);
        newOrder.setJob(job);
        newOrder.setProvision(new Provision(cores, memGb, storageGb, a));
        newOrder.getProvision().setJobUUID(newOrder.getJob().getUuid());
        return newOrder;
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
