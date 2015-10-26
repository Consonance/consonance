package io.consonance.client.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.consonance.client.WebClient;
import io.consonance.client.mix.JobMixIn;
import io.consonance.common.Constants;
import io.consonance.common.Utilities;
import io.swagger.client.ApiException;
import io.swagger.client.api.ConfigurationApi;
import io.swagger.client.api.OrderApi;
import io.swagger.client.model.ExtraFile;
import io.swagger.client.model.Job;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Based on the SeqWare command.line.
 */
public class Main {

    private static final ObjectMapper OBJECT_MAPPER;
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static final HierarchicalINIConfiguration CONFIG;
    static {
        File configFile = new File(System.getProperty("user.home"), ".consonance/config");
        CONFIG = Utilities.parseConfig(configFile.getAbsolutePath());
    }

    private WebClient webClient = null;

    private static void out(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    private static void err(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    public WebClient getWebClient() {
        return webClient;
    }

    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
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

        for (int i = 0; i < args.size(); /** do nothing */ i = i) {
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
            kill("consonance: missing required flag '%s'.", key);
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
    public static final AtomicBoolean QUIET = new AtomicBoolean(false);

    private static String serialize(Object obj) throws ApiException {
        try {
            if (obj != null) {
                return OBJECT_MAPPER.writeValueAsString(obj);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // COMMANDS:

    /**
     * Prints to the console without applying any formatting. Useful for situations where output contains unintended formatting strings,
     * which would break the {@link #out(String format, Object... args)} function. For example, if you try to print an INI file containing
     * the line "refExclude=XX,GL%,hs37d5,XX_001234" the <i>substring</i> "%,h" will cause String.format to throw an exception and fail. So
     * it is sometimes necessary to print output with no consideration to formatting.
     *
     * @param output what to send to System.out
     */
    private static void outWithoutFormatting(String output) {
        System.out.println(output);
    }

    private static void jobStatus(List<String> args, OrderApi jobApi) {
        if (isHelp(args, true)) {
            out("");
            out("Usage: consonance status --help");
            out("       consonance status <params>");
            out("");
            out("Description:");
            out("  List the status of a given job.");
            out("");
            out("Required parameters (one of):");
            out("  --job_uuid <job_uuid>  The UUID of the job");
            out("");
        } else {
            String jobUuid = reqVal(args, "--job_uuid");
            try {
                final Job workflowRun = jobApi.getWorkflowRun(jobUuid);
                if (workflowRun == null){
                    kill("consonance: could not retrieve status of '%s'.", jobUuid);
                }
                outWithoutFormatting(serialize(workflowRun));
            } catch (ApiException e) {
                kill("consonance: could not retrieve status of '%s'.", jobUuid);
            }
        }
    }

    private static void jobSchedule(List<String> args, OrderApi jobApi) {
        if (isHelp(args, true)) {
            out("");
            out("Usage: consonance run --help");
            out("       consonance run <params>");
            out("");
            out("Description:");
            out("  Schedule a job to be run.");
            out("");
            out("Required parameters (one of):");
            out("  --flavour <flavour>              The type of machine that the job should execute on");
            out("  --image-descriptor <file>        Path to the image descriptor, supports http");
            out("  --run-descriptor <file>          Path to the runtime descriptor, supports http");
            out("Optional parameters:");
            out("  --extra-file <path=file=keep>    The path where a particular file should be provisioned, a path to the contents "
                    + "of that file, and whether this file should be kept after execution. Can repeat to specify multiple files");
            out("");
        } else {
            String flavour = reqVal(args, "--flavour");
            String imageDescriptor = reqVal(args, "--image-descriptor");
            String runDescriptor = reqVal(args, "--run-descriptor");
            List<String> extraFiles = optVals(args, "--extra-file");
            try {
                Job job = new Job();
                job.setFlavour(flavour);
                // attempt to read descriptors from URIs
                try {
                    URL jobURL = new URL(imageDescriptor);
                    final Path tempFile = Files.createTempFile("image", "cwl");
                    FileUtils.copyURLToFile(jobURL, tempFile.toFile());
                    job.setContainerImageDescriptor(FileUtils.readFileToString(tempFile.toFile()));
                } catch (IOException e){
                    job.setContainerImageDescriptor(FileUtils.readFileToString(new File(imageDescriptor)));
                }

                try {
                    URL runURL = new URL(runDescriptor);
                    final Path tempFile = Files.createTempFile("run", "json");
                    FileUtils.copyURLToFile(runURL, tempFile.toFile());
                    job.setContainerRuntimeDescriptor(FileUtils.readFileToString(tempFile.toFile()));
                } catch (IOException e){
                    job.setContainerRuntimeDescriptor(FileUtils.readFileToString(new File(runDescriptor)));
                }

                // grab extra files from this run
                for(String extraFile : extraFiles){
                    parseExtraFiles(job, extraFile);
                }
                // grab globally defined extra files
                CONFIG.setListDelimiter(',');
                final String[] extraFilesGlobal = CONFIG.getStringArray(Constants.WEBSERVICE_EXTRA_FILES);
                for(String extraFile : extraFilesGlobal){
                    parseExtraFiles(job, extraFile);
                }

                final Job workflowRun = jobApi.addOrder(job);
                if (workflowRun == null){
                    kill("consonance: failure reading back scheduled job");
                }
                outWithoutFormatting(serialize(workflowRun));
            } catch (ApiException e) {
                kill("consonance: could not schedule");
            } catch (IOException e) {
                kill("consonance: could not read file");
            }
        }
    }

    private static void parseExtraFiles(Job job, String extraFile) throws IOException {
        String[] values = extraFile.split("=");
        final int lengthOfValues = 3;
        if (values.length != lengthOfValues){
            kill("consonance: failure parsing: '%s'.", extraFile);
        }
        ExtraFile file =  new ExtraFile();
        file.setContents(FileUtils.readFileToString(new File(values[1])));
        file.setKeep(Boolean.valueOf(values[2]));
        job.getExtraFiles().put(values[0],file);
    }

    protected void runMain(String[] argv)
            throws OperationNotSupportedException, IOException, TimeoutException, ApiException {
        List<String> args = new ArrayList<>(Arrays.asList(argv));
        if (flag(args, "--debug") || flag(args, "--d")) {
            DEBUG.set(true);
        }
        if (flag(args, "--quiet") || flag(args, "--q")) {
            QUIET.set(true);
            OBJECT_MAPPER.addMixIn(Job.class, JobMixIn.class);
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
            out("  --quiet       Print minimal information");
            out("  --help        Print help out");
            out("  --debug       Print debugging information");
            out("  --version     Print Consonance's version");
            out("  --metadata    Print metadata environment");
            out("");
        } else {
            try {
                String cmd = args.remove(0);
                WebClient client;
                if (this.getWebClient() == null){
                    client = new WebClient(CONFIG);
                } else{
                    client = this.getWebClient();
                }
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
                        jobStatus(args, new OrderApi(client));
                        break;
                    case "update":
                        throw new OperationNotSupportedException("Not implemented yet");
                        //break;
                    case "run":
                        jobSchedule(args, new OrderApi(client));
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

    public static void main(String[] argv) throws IOException, TimeoutException, ApiException, OperationNotSupportedException {
        Main main = new Main();
        main.runMain(argv);
    }
}
