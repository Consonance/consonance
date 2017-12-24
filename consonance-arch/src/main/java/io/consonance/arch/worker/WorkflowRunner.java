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

package io.consonance.arch.worker;

import io.dockstore.client.cli.Client;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * This class will make a command-line call to run a workflow. <br/>
 * The actual command to execute should be specified using setCommandLine. <br/>
 * It is also possible to specify a brief delay before and after the command is executed, using setPreworkDelay and setPostworkDelay.
 *
 * @author sshorser
 *
 */
public class WorkflowRunner implements Callable<WorkflowResult> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowRunner.class);
    private long preworkDelay;
    private long postworkDelay;
    private final CollectingLogOutputStream outputStream = new CollectingLogOutputStream();
    private final CollectingLogOutputStream errorStream = new CollectingLogOutputStream();
    private String configFilePath;
    private String imageDescriptorPath;
    private String runtimeDescriptorPath;
    private static final int DEFAULT_OUTPUT_LINE_LIMIT = 1000;

    /**
     * Get the last *n* lines of output.
     *
     * @param n
     *            - the number of lines to get.
     * @return A string with *n* lines.
     */
    String getStdOut(int n) {
        return StringUtils.join(this.outputStream.getLastNLines(n), "\n");
    }

    /**
     * Get the last *n* lines of output.
     *
     * @param n
     *            - the number of lines to get.
     * @return A string with *n* lines.
     */
    String getStdErr(int n) {
        return StringUtils.join(this.errorStream.getLastNLines(n), "\n");
    }


    @Override
    public WorkflowResult call() throws IOException, ConfigurationException {
        LOG.info("Executing Dockstore CLI programmatically:");
        LOG.info("Image descriptor is: " + imageDescriptorPath);
        LOG.info("Runtime descriptor is: " + runtimeDescriptorPath);
        LOG.info("Config is: " + configFilePath);
        WorkflowResult result = new WorkflowResult();

        // TODO: wrap with try
        //LauncherCWL launcher = new LauncherCWL(configFilePath, imageDescriptorPath, runtimeDescriptorPath, outputStream, errorStream);

        try {
            if (this.preworkDelay > 0) {
                LOG.info("Sleeping before executing workflow for " + this.preworkDelay + " ms.");
                Thread.sleep(this.preworkDelay);
            }
            // this is a blocking call, but the HeartbeatThread appears to be a in a separate thread
            final String[] s = { "workflow", "launch", "--local-entry", imageDescriptorPath, "--json", runtimeDescriptorPath};
            try {
                LOG.info("command: dockstore "+ String.join(" ", s));
                // see https://stackoverflow.com/questions/5389632/capturing-contents-of-standard-output-in-java for how I redirected stderr/out of the below
                PrintStream originalStdOut = System.out;
                PrintStream originalStdErr = System.err;
                System.setOut(new PrintStream(this.outputStream, true, "UTF-8"));
                System.setErr(new PrintStream(this.errorStream, true, "UTF-8"));
                // this is the actual call to Dockstore CLI
                // TODO: how do I set DOCKSTORE_ROOT=1?
                Client.main(s);
                System.setOut(originalStdOut);
                System.setErr(originalStdErr);

            } catch (NoClassDefFoundError e) {
                e.printStackTrace();
            }

            // FIXME: this needs to use the streams from the Dockstore CLI
            result.setWorkflowStdout(this.getStdOut(DEFAULT_OUTPUT_LINE_LIMIT));
            result.setWorkflowStdErr(this.getStdErr(DEFAULT_OUTPUT_LINE_LIMIT));
            // exit code is artificial since the CWL runner actually runs more than one command
            // we use 0 to indicate success, 1 to indicate failure (and the streams will have more detail)
            // TODO: I don't see any logic here for checking the actual error code!!!
            result.setExitCode(0);
            if (this.postworkDelay > 0) {
                LOG.info("Sleeping after executing workflow for " + this.postworkDelay + " ms.");
                Thread.sleep(this.postworkDelay);
            }
        } catch (InterruptedException | RuntimeException e) {
            LOG.error("Worker interrupt or runtime exception!!! "+e.getMessage(), e);
            result.setWorkflowStdout(this.getStdOut(DEFAULT_OUTPUT_LINE_LIMIT));
            result.setWorkflowStdout(this.getStdErr(DEFAULT_OUTPUT_LINE_LIMIT));
            // if there's an exception then indicate via exit code
            result.setExitCode(1);
        } catch (Exception e) {
            LOG.error("Worker general exception!!! "+e.getMessage(), e);
            result.setWorkflowStdout(this.getStdOut(DEFAULT_OUTPUT_LINE_LIMIT));
            result.setWorkflowStdout(this.getStdErr(DEFAULT_OUTPUT_LINE_LIMIT));
            // if there's an exception then indicate via exit code
            result.setExitCode(1);
        } finally {
            this.outputStream.close();
            this.errorStream.close();
        }
        LOG.debug("Workflowrunner exiting");
        return result;
    }

    void setPreworkDelay(long preworkDelay) {
        this.preworkDelay = preworkDelay;
    }

    void setPostworkDelay(long postworkDelay) {
        this.postworkDelay = postworkDelay;
    }

    void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    void setImageDescriptorPath(String imageDescriptorPath) {
        this.imageDescriptorPath = imageDescriptorPath;
    }

    void setRuntimeDescriptorPath(String runtimeDescriptorPath) {
        this.runtimeDescriptorPath = runtimeDescriptorPath;
    }
}
