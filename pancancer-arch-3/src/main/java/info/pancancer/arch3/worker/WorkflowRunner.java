package info.pancancer.arch3.worker;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will make a command-line call to run a workflow. <br/>
 * The actual command to execute should be specified using setCommandLine. <br/>
 * It is also possible to specify a brief delay before and after the command is executed, using setPreworkDelay and setPostworkDelay.
 *
 * @author sshorser
 *
 */
public class WorkflowRunner implements Callable<WorkflowResult> {
    protected static final Logger LOG = LoggerFactory.getLogger(WorkflowRunner.class);
    private long preworkDelay;
    private long postworkDelay;
    private CommandLine cli;
    private final CollectingLogOutputStream outputStream = new CollectingLogOutputStream();
    private final CollectingLogOutputStream errorStream = new CollectingLogOutputStream();

    /**
     * Get the stdout of the running command.
     *
     * @return
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
     * @return
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
        PumpStreamHandler streamHandler = new PumpStreamHandler(this.outputStream, this.errorStream);
        WorkflowResult result = new WorkflowResult();

        DefaultExecutor executor = new DefaultExecutor();

        /*
         * CommandLine cli = new CommandLine("docker"); cli.addArguments(new String[] { "run", "--rm", "-h", "master", "-t" ,"-v",
         * "/var/run/docker.sock:/var/run/docker.sock", "-v", job.getWorkflowPath() + ":/workflow", "-v",pathToINI + ":/ini", "-v",
         * "/datastore:/datastore", "-v","/home/"+this.userName+"/.ssh/gnos.pem:/home/ubuntu/.ssh/gnos.pem",
         * "seqware/seqware_whitestar_pancancer", "seqware", "bundle", "launch", "--dir", "/workflow", "--ini", "/ini", "--no-metadata" });
         */
        LOG.info("Executing command: " + this.cli.toString().replace(",", ""));

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        executor.setStreamHandler(streamHandler);

        try {
            if (this.preworkDelay > 0) {
                LOG.info("Sleeping before executing workflow for " + this.preworkDelay + " ms.");
                Thread.sleep(this.preworkDelay);
            }

            executor.execute(cli, resultHandler);
            // Use the result handler for non-blocking call, so this way we should be able to get updates of
            // stdout and stderr while the command is running.
            resultHandler.waitFor();
            result.setWorkflowStdout(outputStream.getAllLinesAsString());
            result.setWorkflowStdErr(errorStream.getAllLinesAsString());
            result.setExitCode(resultHandler.getExitValue());
            if (this.postworkDelay > 0) {
                LOG.info("Sleeping after executing workflow for " + this.postworkDelay + " ms.");
                Thread.sleep(this.postworkDelay);
            }
        } catch (ExecuteException e) {
            LOG.error(e.getMessage(), e);
            result.setWorkflowStdout(this.getStdErr());
        } catch (InterruptedException | IOException e) {
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

    public CommandLine getCli() {
        return cli;
    }

    public void setCli(CommandLine cli) {
        this.cli = cli;
    }

}
