package io.consonance.client.cli;

import com.google.common.base.Joiner;
import com.google.common.collect.ObjectArrays;
import io.consonance.client.WebClient;
import io.swagger.client.ApiException;
import io.swagger.client.JSON;
import io.swagger.client.api.ConfigurationApi;
import io.swagger.client.api.JobApi;
import io.swagger.client.model.Job;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Based on the SeqWare command.line.
 */
public class Main {


    private static void out(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    private static void err(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    private static class Kill extends RuntimeException {
    }

    private static void kill(String format, Object... args) {
        err(format, args);
        throw new Kill();
    }

    private static void invalid(String cmd) {
        kill("consonance: '%s' is not a consonance command. See 'consonance --help'.", cmd);
    }

    private static void invalid(String cmd, String sub) {
        kill("consonance: '%s %s' is not a consonance command. See 'consonance %s --help'.", cmd, sub, cmd);
    }

    private static boolean flag(List<String> args, String flag) {
        boolean found = false;
        for (int i = 0; i < args.size(); i++) {
            if (flag.equals(args.get(i))) {
                if (found) {
                    kill("consonance: multiple instances of '%s'.", flag);
                } else {
                    found = true;
                    args.remove(i);
                }
            }
        }
        return found;
    }

    private static List<String> optVals(List<String> args, String key) {
        List<String> vals = new ArrayList<>();

        for (int i = 0; i < args.size();) {
            String s = args.get(i);
            if (key.equals(s)) {
                args.remove(i);
                if (i < args.size()) {
                    String val = args.remove(i);
                    if (!val.startsWith("--")) {
                        String[] ss = val.split(",");
                        if (ss.length > 0) {
                            vals.addAll(Arrays.asList(ss));
                            continue;
                        }
                    }
                }
                kill("seqware: missing required argument to '%s'.", key);
            } else {
                i++;
            }
        }

        return vals;
    }

    private static List<String> reqVals(List<String> args, String key) {
        List<String> vals = optVals(args, key);

        if (vals.isEmpty()) {
            kill("seqware: missing required flag '%s'.", key);
        }

        return vals;
    }

    private static String optVal(List<String> args, String key, String defaultVal) {
        String val = defaultVal;

        List<String> vals = optVals(args, key);
        if (vals.size() == 1) {
            val = vals.get(0);
        } else if (vals.size() > 1) {
            kill("consonance: multiple instances of '%s'.", key);
        }

        return val;
    }

    private static String reqVal(List<String> args, String key) {
        String val = optVal(args, key, null);

        if (val == null) {
            kill("consonance: missing required flag '%s'.", key);
        }

        return val;
    }

    private static boolean isHelp(List<String> args, boolean valOnEmpty) {
        if (args.isEmpty()) {
            return valOnEmpty;
        }

        String first = args.get(0);
        return first.equals("-h") || first.equals("--help");
    }

    public static final AtomicBoolean DEBUG = new AtomicBoolean(false);
    public static final AtomicBoolean VERBOSE = new AtomicBoolean(false);

    private static void run(String... args) {
        if (VERBOSE.get()) {
            args = ObjectArrays.concat("--verbose", args);
        }
        if (DEBUG.get()) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].contains(" ")) {
                    args[i] = "'" + args[i] + "'";
                }
            }
            out("PluginRunner.main: %s", Joiner.on(",").join(args));
        } else {
            System.out.println("Do it");
            //PluginRunner.main(args);
        }
    }

    private static void run(List<String> runnerArgs) {
        run(runnerArgs.toArray(new String[runnerArgs.size()]));
    }

    // COMMANDS:


    /**
     * Prints to the console without applying any formatting. Useful for situations where output contains unintended formatting strings,
     * which would break the {@link #out(String format, Object... args)} function. For example, if you try to print an INI file containing
     * the line "refExclude=XX,GL%,hs37d5,XX_001234" the <i>substring</i> "%,h" will cause String.format to throw an exception and fail. So
     * it is sometimes necessary to print output with no consideration to formatting.
     *
     * @param output
     */
    private static void outWithoutFormatting(String output) {
        System.out.println(output);
    }

    private static void jobStatus(List<String> args, JobApi jobApi) {
        if (isHelp(args, true)) {
            out("");
            out("Usage: consonance status --help");
            out("       consonance status <params>");
            out("");
            out("Description:");
            out("  List the status of a given job.");
            out("");
            out("Required parameters (one of):");
            out("  --job <uuid>  The UUID of the job");
            out("");
        } else {
            String jobUuid = reqVal(args, "--job");
            try {
                JSON json = new JSON();
                final Job workflowRun = jobApi.getWorkflowRun(jobUuid);
                out(json.serialize(workflowRun));
            } catch (ApiException e) {
                kill("consonance: could not retrieve status of '%s'.", jobUuid);
            }
        }
    }



    public static void main(String[] argv) throws IOException, TimeoutException, ApiException, OperationNotSupportedException {
        List<String> args = new ArrayList<>(Arrays.asList(argv));
        if (flag(args, "--debug")) {
            DEBUG.set(true);
        }
        if (flag(args, "--verbose")) {
            VERBOSE.set(true);
            throw new OperationNotSupportedException("Not implemented yet");
        }

        if (isHelp(args, true)) {
            out("");
            out("Usage: consonance [<flag>]");
            out("       consonance <command> [--help]");
            out("");
            out("Commands:");
            out("  run           Schedule a job");
            out("  status        Get the status of a job");
            out("  update        Update this tool to a newer version");
            // out("  dev           Advanced commands that are useful for developers or debugging");
            out("");
            out("Flags:");
            out("  --help        Print help out");
            // handled in seqware script:
            out("  --version     Print Consonance's version");
            out("  --metadata    Print metadata environment");
            out("");
        } else {
            try {
                String cmd = args.remove(0);
                HierarchicalINIConfiguration config = new HierarchicalINIConfiguration();
                WebClient client = new WebClient(config);
                client.setDebugging(DEBUG.get());

                if (null != cmd) {
                    switch (cmd) {
                    case "-v":
                    case "--version":
                        kill("consonance: version information is provided by the wrapper script.");
                        break;
                    case "--metadata":
                        ConfigurationApi configApi = new ConfigurationApi(client);
                        out(configApi.listConfiguration());
                        break;
                    case "status":
                        jobStatus(args, new JobApi(client));
                        break;
                    case "update":
                        throw new OperationNotSupportedException("Not implemented yet");
                        break;
                    case "run":
                        runJob(args);
                        break;
                    default:
                        invalid(cmd);
                        break;
                    }
                }
            } catch (Kill k) {
                System.exit(1);
            }
        }
    }
}
