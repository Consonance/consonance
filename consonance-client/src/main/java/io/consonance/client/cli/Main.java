package io.consonance.client.cli;

import com.google.common.base.Joiner;
import com.google.common.collect.ObjectArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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


    private static void runJob(List<String> args) {
        if (isHelp(args, true)) {
            out("");
            out("Usage: seqware workflow-run --help");
            out("       seqware workflow-run <sub-command> [--help]");
            out("");
            out("Description:");
            out("  Interact with workflow runs.");
            out("");
            out("Sub-commands:");
            out("  cancel              Cancel a submitted or running workflow run");
            out("  launch-scheduled    Launch scheduled workflow runs");
            out("  propagate-statuses  Propagate workflow engine statuses to seqware meta DB");
            out("  retry               Retry a failed or cancelled workflow run skipping completed steps");
            out("  reschedule          Reschedule a workflow-run to re-run from scratch as a new run");
            out("  stderr              Obtain the stderr output of the run");
            out("  stdout              Obtain the stdout output of the run");
            out("  report              The details of a given workflow-run");
            out("  watch               Watch a workflow-run in progress");
            out("  ini                 Output the effective ini for a workflow run");
            out("  delete              Recursively delete workflow-runs");
            out("");
        } else {
            String cmd = args.remove(0);
            if (null != cmd) {
                switch (cmd) {

                default:
                    invalid("run", cmd);
                    break;
                }
            }
        }
    }

    public static void main(String[] argv) {
        List<String> args = new ArrayList<>(Arrays.asList(argv));
        if (flag(args, "--debug")) {
            DEBUG.set(true);
        }
        if (flag(args, "--verbose")) {
            VERBOSE.set(true);
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
                if (null != cmd) {
                    switch (cmd) {
                    case "-v":
                    case "--version":
                        kill("seqware: version information is provided by the wrapper script.");
                        break;
                    case "--metadata":
                        break;
                    case "status":
                        //query(args);
                        break;
                    case "update":
                        //bundle(args);
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
