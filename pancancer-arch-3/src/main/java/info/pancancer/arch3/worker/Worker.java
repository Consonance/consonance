package info.pancancer.arch3.worker;

import info.pancancer.arch3.Base;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

/**
 * Wraps WorkerRunnable which does not have a command-line interface.
 *
 * @author dyuen
 */
public class Worker extends Base {

    private final OptionSpecBuilder testSpec;
    private final ArgumentAcceptingOptionSpec<Integer> maxRunsSpec;
    private final ArgumentAcceptingOptionSpec<String> uuidSpec;
    private final ArgumentAcceptingOptionSpec<String> pidFileSpec;
    private static OptionParser optParser;

    private Worker(String[] argv) throws IOException {
        super();
        this.testSpec = super.parser.accepts("test", "In test mode, worker simply completes successfully");
        this.configSpec = parser.accepts("config", "Specify a config file").withRequiredArg().required().ofType(String.class);
        //TODO: Implement the "endless" logic for the worker? Or change the command-line parser to not accept "--endless" for Worker, which requires changes in Base.
        this.maxRunsSpec = parser.accepts("max-runs","The maximum number of workflows to execute. If \"--endless\" is set, this number will be ignored.").withOptionalArg().ofType(Integer.class).defaultsTo(1);
        // UUID should be required: if the user doesn't specify it, there's no other way for the VM to get a UUID
        this.uuidSpec = parser.accepts("uuid","The UUID of this VM.").withRequiredArg().required().ofType(String.class).required().defaultsTo("/var/run/arch3_worker.pid");
        this.pidFileSpec = parser.accepts("pidFile", "Path to lock file.").withRequiredArg().required().ofType(String.class);
        parser.accepts("help").forHelp();
        optParser = parser;
        super.parseOptions(argv);
    }

    public static void main(String[] argv) throws Exception {
        final Worker worker = new Worker(argv);
        OptionSet options = worker.options;
        if (options.has("help"))
        {
            optParser.printHelpOn(System.out);
        }
        else
        {
            if (options.has(worker.pidFileSpec)) {
                final String pidFile = options.valueOf(worker.pidFileSpec);
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        worker.log.info("Worker caught a shutdown signal, shutting down now!");
                        if (pidFile != null && pidFile.trim().length() > 0) {
                            Path pidPath = Paths.get(pidFile);
                            try {
                                Files.delete(pidPath);
                            } catch (IOException e) {
                                worker.log.error("Unable to delete PID file: " + pidFile + " , message: " + e.getMessage());
                                worker.log.error("You may have to delete the PID file manually to run the worker again.");
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            WorkerRunnable workerRunnable = new WorkerRunnable(options.valueOf(worker.configSpec), options.valueOf(worker.uuidSpec),
                    options.valueOf(worker.maxRunsSpec), options.has(worker.testSpec));
            workerRunnable.run();
            worker.log.info("Exiting.");
        }
    }
}
