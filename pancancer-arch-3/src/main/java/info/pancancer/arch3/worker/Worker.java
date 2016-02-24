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
        this.testSpec = super.parser.accepts("test", "In test mode, worker simply completes successfully");
        this.configSpec = parser.accepts("config", "Specify a config file").withRequiredArg().required().ofType(String.class);
        this.maxRunsSpec = parser
                .accepts("max-runs", "The maximum number of workflows to execute. If \"--endless\" is set, this number will be ignored.")
                .withOptionalArg().ofType(Integer.class).defaultsTo(1);
        // UUID is now optional, we can read cloud init
        this.uuidSpec = parser.accepts("uuid", "The UUID of this VM.").withRequiredArg().ofType(String.class).required();
        this.pidFileSpec = parser.accepts("pidFile", "Path to lock file.").withRequiredArg().ofType(String.class)
                .defaultsTo("/var/run/arch3_worker.pid");
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
                        }
                    }
                }
            });
        }
        WorkerRunnable workerRunnable = new WorkerRunnable(options.valueOf(worker.configSpec), options.valueOf(worker.uuidSpec),
                options.valueOf(worker.maxRunsSpec), options.has(worker.testSpec), options.has(worker.endlessSpec));
        workerRunnable.run();
        worker.log.info("Exiting.");
    }
}
