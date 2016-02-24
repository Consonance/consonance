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

import io.github.collaboratory.LauncherCWL;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    public static final int DEFAULT_OUTPUT_LINE_LIMIT = 1000;

    /**
     * Get the last *n* lines of output.
     *
     * @param n
     *            - the number of lines to get.
     * @return A string with *n* lines.
     */
    public String getStdOut(int n) {
        return StringUtils.join(this.outputStream.getLastNLines(n), "\n");
    }

    /**
     * Get the last *n* lines of output.
     *
     * @param n
     *            - the number of lines to get.
     * @return A string with *n* lines.
     */
    public String getStdErr(int n) {
        return StringUtils.join(this.errorStream.getLastNLines(n), "\n");
    }


    @Override
    public WorkflowResult call() throws IOException {
        LOG.info("Executing cwlLauncher:");
        LOG.info("Image descriptor is: " + imageDescriptorPath);
        LOG.info("Runtime descriptor is: " + runtimeDescriptorPath);
        LOG.info("Config is: " + configFilePath);
        WorkflowResult result = new WorkflowResult();

        LauncherCWL launcher = new LauncherCWL(configFilePath, imageDescriptorPath, runtimeDescriptorPath, this.outputStream, this.errorStream);

        try {
            if (this.preworkDelay > 0) {
                LOG.info("Sleeping before executing workflow for " + this.preworkDelay + " ms.");
                Thread.sleep(this.preworkDelay);
            }
            // this is a blocking call, but the HeartbeatThread appears to be a in a separate thread
            launcher.run();
            result.setWorkflowStdout(this.getStdOut(DEFAULT_OUTPUT_LINE_LIMIT));
            result.setWorkflowStdErr(this.getStdErr(DEFAULT_OUTPUT_LINE_LIMIT));
            // exit code is artificial since the CWL runner actually runs more than one command
            // we use 0 to indicate success, 1 to indicate failure (and the streams will have more detail)
            result.setExitCode(0);
            if (this.postworkDelay > 0) {
                LOG.info("Sleeping after executing workflow for " + this.postworkDelay + " ms.");
                Thread.sleep(this.postworkDelay);
            }
        } catch (InterruptedException | RuntimeException e) {
            LOG.error(e.getMessage(), e);
            result.setWorkflowStdout(this.getStdErr(DEFAULT_OUTPUT_LINE_LIMIT));
        } finally {
            this.outputStream.close();
            this.errorStream.close();
        }
        LOG.debug("Workflowrunner exiting");
        return result;
    }

    public long getPreworkDelay() {
        return preworkDelay;
    }

    public void setPreworkDelay(long preworkDelay) {
        this.preworkDelay = preworkDelay;
    }

    public long getPostworkDelay() {
        return postworkDelay;
    }

    public void setPostworkDelay(long postworkDelay) {
        this.postworkDelay = postworkDelay;
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public String getImageDescriptorPath() {
        return imageDescriptorPath;
    }

    public void setImageDescriptorPath(String imageDescriptorPath) {
        this.imageDescriptorPath = imageDescriptorPath;
    }

    public String getRuntimeDescriptorPath() {
        return runtimeDescriptorPath;
    }

    public void setRuntimeDescriptorPath(String runtimeDescriptorPath) {
        this.runtimeDescriptorPath = runtimeDescriptorPath;
    }
}
