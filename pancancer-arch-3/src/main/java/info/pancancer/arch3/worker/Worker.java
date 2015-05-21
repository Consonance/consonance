package info.pancancer.arch3.worker;

import info.pancancer.arch3.Base;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.ArgumentAcceptingOptionSpec;
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

    private Worker(String[] argv) throws IOException {
        super();
        this.testSpec = super.parser.accepts("test", "in test mode, worker simply completes successfully");
        this.configSpec = parser.accepts("config", "specify a config file").withOptionalArg().ofType(String.class);
        this.maxRunsSpec = parser.accepts("max-runs").withOptionalArg().ofType(Integer.class).defaultsTo(1);
        // UUID should be required: if the user doesn't specify it, there's no other way for the VM to get a UUID
        this.uuidSpec = parser.accepts("uuid").withRequiredArg().ofType(String.class).required();
        this.pidFileSpec = parser.accepts("pidFile", "path to lock file").withRequiredArg().ofType(String.class);
        super.parseOptions(argv);
    }

    public static void main(String[] argv) throws Exception {
        final Worker worker = new Worker(argv);
        OptionSet options = worker.options;
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
