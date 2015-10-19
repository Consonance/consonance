package io.consonance.arch.worker;

import io.github.collaboratory.LauncherCWL;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    /**
     * Get the stdout of the running command.
     *
     * @return stdout
     */
    public String getStdOut() {
        String s;
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            this.outputStream.flush();
            s = this.outputStream.getAllLinesAsString();
        } finally {
            lock.unlock();
        }
        return s;
    }

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
     * Get the stderr of the running command.
     *
     * @return stderr
     */
    public String getStdErr() {
        String s;
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            this.errorStream.flush();
            s = this.errorStream.getAllLinesAsString();
        } finally {
            lock.unlock();
        }
        return s;
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
            result.setWorkflowStdout(outputStream.getAllLinesAsString());
            result.setWorkflowStdErr(errorStream.getAllLinesAsString());
            // exit code is artificial since the CWL runner actually runs more than one command
            // we use 0 to indicate success, 1 to indicate failure (and the streams will have more detail)
            result.setExitCode(0);
            if (this.postworkDelay > 0) {
                LOG.info("Sleeping after executing workflow for " + this.postworkDelay + " ms.");
                Thread.sleep(this.postworkDelay);
            }
        } catch (InterruptedException | RuntimeException e) {
            LOG.error(e.getMessage(), e);
            result.setWorkflowStdout(this.getStdErr());
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
