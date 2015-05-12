package info.pancancer.arch3.worker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will make a command-line call to run a workflow. <br/>
 * The actual command to execute should be specified using setCommandLine. <br/>
 * It is also possible to specify a brief delay before and after the command is executed, using setPreworkDelay and setPostworkDelay.
 * 
 * @author sshorser
 *
 */
public class WorkflowRunner implements Callable<String> {

    public class CollectingLogOutputStream extends LogOutputStream {
        private final List<String> lines = new LinkedList<String>();

        @Override
        protected void processLine(String line, int level) {
            Lock lock = new ReentrantLock();
            lock.lock();
            lines.add(line);
            lock.unlock();
        }

        public String getAllLinesAsString() {
            Lock lock = new ReentrantLock();
            lock.lock();
            String allTheLines = StringUtils.join(this.lines, "\n");
            lock.unlock();
            return allTheLines;
        }

        public List<String> getLastNLines(int n) {
            Lock lock = new ReentrantLock();
            lock.lock();
            List<String> nlines = new ArrayList<String>(n);
            int start, end;
            end = this.lines.size();
            start = Math.max(0, this.lines.size() - n);
            if (end > start && start >= 0) {
                nlines = this.lines.subList(start, end);
            }
            lock.unlock();
            return nlines;
        }
    }

    private long preworkDelay;
    private long postworkDelay;
    private CommandLine cli;
    // private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // private ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private CollectingLogOutputStream outputStream = new CollectingLogOutputStream();
    private CollectingLogOutputStream errorStream = new CollectingLogOutputStream();

    /**
     * Get the stdout of the running command.
     * 
     * @return
     */
    public String getStdOut() {
        String s;
        Lock lock = new ReentrantLock();
        lock.lock();
        this.outputStream.flush();
        s = this.outputStream.getAllLinesAsString();
        lock.unlock();
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
        this.errorStream.flush();
        return errorStream.getAllLinesAsString();
    }

    @Override
    public String call() throws Exception {
        PumpStreamHandler streamHandler = new PumpStreamHandler(this.outputStream, this.errorStream);
        String workflowOutput = "";

        DefaultExecutor executor = new DefaultExecutor();

        /*
         * CommandLine cli = new CommandLine("docker"); cli.addArguments(new String[] { "run", "--rm", "-h", "master", "-t" ,"-v",
         * "/var/run/docker.sock:/var/run/docker.sock", "-v", job.getWorkflowPath() + ":/workflow", "-v",pathToINI + ":/ini", "-v",
         * "/datastore:/datastore", "-v","/home/"+this.userName+"/.ssh/gnos.pem:/home/ubuntu/.ssh/gnos.pem",
         * "seqware/seqware_whitestar_pancancer", "seqware", "bundle", "launch", "--dir", "/workflow", "--ini", "/ini", "--no-metadata" });
         */
        System.out.println("Executing command: " + this.cli.toString().replace(",", ""));

        if (this.preworkDelay > 0) {
            System.out.println("Sleeping before executing workflow for " + this.preworkDelay + " ms.");
            Thread.sleep(this.preworkDelay);
        }
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        executor.setStreamHandler(streamHandler);
        executor.execute(cli, resultHandler);
        // Use the result handler for non-blocking call, so this way we should be able to get updates of
        // stdout and stderr while the command is running.
        resultHandler.waitFor();
        workflowOutput = outputStream.getAllLinesAsString();
        if (this.postworkDelay > 0) {
            System.out.println("Sleeping after exeuting workflow for " + this.postworkDelay + " ms.");
            Thread.sleep(this.postworkDelay);
        }

        this.outputStream.close();

        this.errorStream.close();

        return workflowOutput;
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
