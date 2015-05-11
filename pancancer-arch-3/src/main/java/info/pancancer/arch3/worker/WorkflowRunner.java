package info.pancancer.arch3.worker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * This class will make a command-line call to run a workflow. <br/>
 * The actual command to execute should be specified using setCommandLine. <br/>
 * It is also possible to specify a brief delay before and after the command is executed, using setPreworkDelay and setPostworkDelay.
 * 
 * @author sshorser
 *
 */
public class WorkflowRunner implements Callable<String> {

    private long preworkDelay;
    private long postworkDelay;
    private CommandLine cli;

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

    /**
     * Get the stdout of the running command.
     * 
     * @return
     */
    public String getStdOut() {
        try {
            this.outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Get the stderr of the running command.
     * 
     * @return
     */
    public String getStdErr() {
        try {
            this.errorStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(errorStream.toByteArray(), StandardCharsets.UTF_8);
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
        workflowOutput = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
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
