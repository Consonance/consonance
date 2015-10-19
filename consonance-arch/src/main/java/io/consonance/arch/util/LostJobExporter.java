package io.consonance.arch.util;

import io.consonance.arch.Base;
import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.common.CommonTestUtilities;
import joptsimple.ArgumentAcceptingOptionSpec;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This can export ini files belonging to lost or failed jobs.
 *
 * Lost jobs are switched to failed status (i.e. presumed lost is the same as failed)
 */
public class LostJobExporter extends Base {

    // variables
    private HierarchicalINIConfiguration settings = null;
    private final ArgumentAcceptingOptionSpec<String> idListSpec;
    private final ArgumentAcceptingOptionSpec<String> dirSpec;

    public static void main(String[] argv) throws IOException {
        LostJobExporter jg = new LostJobExporter(argv);
        jg.runExport();
    }

    /**
     * This is ugly, but tiny.
     *
     * @param argv arguments from command-line
     * @throws IOException
     */
    public LostJobExporter(String[] argv) throws IOException {
        super();
        this.idListSpec = super.parser.accepts("job_uuids", "a list of ids of failed or lost jobs for ini files to export")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class);
        this.dirSpec = super.parser.accepts("ini-dir", "create a batch of ini files in this directory").withOptionalArg()
                .ofType(String.class).defaultsTo("ini");

        parseOptions(argv);

        settings = CommonTestUtilities.parseConfig(configFile);
    }

    private void runExport() throws IOException {

        String exportDir = options.valueOf(dirSpec);
        List<String> idsToRecoverList = options.valuesOf(idListSpec);
        Set<String> idsToRecover = new HashSet<>(idsToRecoverList);

        PostgreSQL db = new PostgreSQL(settings);
        List<Job> recoveredJobs = new ArrayList<>();

        for (Job job : db.getJobs(JobState.FAILED)) {
            if (idsToRecover.contains(job.getUuid())) {
                recoveredJobs.add(job);
            }
        }
        for (Job job : db.getJobs(JobState.LOST)) {
            if (idsToRecover.contains(job.getUuid())) {
                recoveredJobs.add(job);
            }
        }
        log.info("Jobs read from DB");
        if (recoveredJobs.size() == idsToRecover.size()) {
            for (Job job : recoveredJobs) {
                File outFile = new File(exportDir, job.getUuid() + ".cwl");
                FileUtils.write(outFile, job.getContainerImageDescriptor(), StandardCharsets.UTF_8);
                File jsonFile = new File(exportDir, job.getUuid() + ".json");
                FileUtils.write(jsonFile, job.getContainerRuntimeDescriptor(), StandardCharsets.UTF_8);
            }
        } else {
            String description = "only recovered " + recoveredJobs.size() + "  out of " + idsToRecover.size();
            log.error(description);
            throw new IllegalArgumentException(description);
        }
        log.info("Jobs written to files");

        // flip state of lost jobs to failed jobs
        log.info("flipping LOST job states to FAILED");
        for (Job job : recoveredJobs) {
            if (job.getState().equals(JobState.LOST)) {
                db.updateJob(job.getUuid(), job.getVmUuid(), JobState.FAILED);
            }
        }
    }

}
