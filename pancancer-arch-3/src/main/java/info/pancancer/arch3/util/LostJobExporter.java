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

package info.pancancer.arch3.util;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.utils.Utilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import joptsimple.ArgumentAcceptingOptionSpec;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;

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
     * @param argv
     * @throws IOException
     */
    public LostJobExporter(String[] argv) throws IOException {
        super();
        this.idListSpec = super.parser.accepts("job_uuids", "a list of ids of failed or lost jobs for ini files to export")
                .withRequiredArg().withValuesSeparatedBy(',').ofType(String.class);
        this.dirSpec = super.parser.accepts("ini-dir", "create a batch of ini files in this directory").withOptionalArg()
                .ofType(String.class).defaultsTo("ini");

        parseOptions(argv);

        settings = Utilities.parseConfig(configFile);
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
                File outFile = new File(exportDir, job.getUuid() + ".ini");
                FileUtils.write(outFile, job.getIniStr(), StandardCharsets.UTF_8);
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
