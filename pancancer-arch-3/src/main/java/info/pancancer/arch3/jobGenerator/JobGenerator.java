package info.pancancer.arch3.jobGenerator;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.json.simple.JSONObject;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.utils.Utilities;

/**
 * Created by boconnor on 15-04-18.
 *
 * takes a --config option pointed to a config .json file
 */
public class JobGenerator extends Base {

    // variables
    private String outputFile = null;
    private JSONObject settings = null;
    private Channel jchannel = null;
    private Channel rchannel = null;
    private Connection connection = null;
    private String queueName = null;
    private ArrayList<JSONObject> resultsArr = null;
    private QueueingConsumer rconsumer = null;
    private Date start = new Date();
    private int currIterations = 0;
    private int overallIterationsMax = 0;
    private int overallRuntimeMaxHours = 0;

    public JobGenerator(String configFile) {
        try {

            // UTILS OBJECT
            Utilities u = new Utilities();
            settings = u.parseConfig(configFile);
            if (outputFile == null) { outputFile = (String) settings.get("results"); }
            u.setupOutputFile(outputFile, settings);
            overallRuntimeMaxHours = ((Number) settings.get("overallRuntimeMaxHours")).intValue();
            overallIterationsMax = ((Number) settings.get("overallIterationsMax")).intValue();
            // Utilities will handle persisting data to disk on exit
            Runtime.getRuntime().addShutdownHook(u);
            resultsArr = u.getResultsArr();

            // CONFIG
            queueName = (String) settings.get("rabbitMQQueueName");
            System.out.println("QUEUE NAME: "+queueName);

            // SETUP QUEUE
            this.jchannel = u.setupQueue(settings, queueName + "_orders");
            this.rchannel = u.setupQueue(settings, queueName + "_results"); // REMOVE

            System.out.println("RCHAN: "+rchannel);

            // RESULTS CONSUMER
            rconsumer = new QueueingConsumer(this.rchannel);
            rchannel.basicConsume(queueName + "_results", true, rconsumer);

            // LOOP, ADDING JOBS EVERY FEW MINUTES
            boolean moreJobs = true;
            while (moreJobs) {

                // keep track of the iterations
                currIterations++;

                System.out.println("GENERATING NEW JOBS");
                // TODO: this is fake, in a real program this is being read from JSONL file or web service
                // check to see if new results are available and/or if the work queue is empty
                String[] newJobs = generateNewJobs("", resultsArr, u);

                // enqueue new jobs if we have them
                if (newJobs.length > 0) {
                    enqueueNewJobs(newJobs);
                } else {
                    System.out.println("CAN'T FIND NEW STATE TO TRY, LIKELY CONVERGED");
                    moreJobs = false;
                }

                // decide to exit
                if (exceededTimeOrJobs() || !moreJobs) {
                    moreJobs = false;
                    System.out.println("TIME OR JOBS EXCEEDED, EXITING");
                } else {
                    try {
                        // pause
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        log.error(ex.toString());
                    }
                }
            }

            try {

                jchannel.getConnection().close(5000);
                rchannel.getConnection().close(5000);

            } catch (IOException ex) {
                log.error(ex.toString());
            }

        } catch (IOException ex) {
            log.error(ex.toString());
        }
    }

    public static void main(String [] args)
    {
        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(args);

        String configFile = null;
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }

        JobGenerator jg = new JobGenerator(configFile);

        jg.log.info("MASTER FINISHED, EXITING!");
    }

    // PRIVATE

    private String[] generateNewJobs(String baseCmd, ArrayList<JSONObject> resultsArr, Utilities u) {
        ArrayList<String> jobs = new ArrayList<String>();
        try {
            int messages = jchannel.queueDeclarePassive(queueName + "_orders").getMessageCount();
            System.out.println("JOB QUEUE SIZE: " + messages);
            // if there are no messages then we'll want to add some new jobs
            if (!exceededTimeOrJobs()) {
                // TODO, actually generate new jobs if the job queue is empty
                String newJob = makeNewJob(baseCmd, resultsArr, u);
                if (newJob != null) { jobs.add(newJob); }
            }

        } catch (IOException ex) {
            log.error(ex.toString());
        }
        return (jobs.toArray(new String[0]));

    }

    private String makeNewJob(String baseCmd, ArrayList<JSONObject> resultsArr, Utilities u) {
        // TODO: this will actually need to come from a file or web service
        return ("{ \n" +
                "  \"uuid\": \"<uuid>\",\n" +
                "  \"job\": {\n" +
                "    \"job_hash\": \"<hash>\",\n" +
                "    \"workflow_name\": \"Sanger\",\n" +
                "    \"workflow_version\" : \"1.0.1\",\n" +
                "    \"arguments\" : {\n" +
                "      \"param1\": \"bar\",\n" +
                "      \"param2\": \"1928\",\n" +
                "      \"param3\": \"abc\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"provision\" : {\n" +
                "    \"cores\": 8,\n" +
                "    \"mem_gb\": 25,\n" +
                "    \"storage_gb\": 1024,\n" +
                "    \"bindle_profiles_to_run\": [\"<list_of_bindle_profiles_aka_anible_scripts>\"],\n" +
                "    \"workflow_zips\": [\"http://s3/workflow.zip\"],\n" +
                "    \"docker_images\": [\"seqware-whitestar\"]\n" +
                "  }\n" +
                "}");
    }

    private boolean exceededTimeOrJobs() {

        // FIXME: hardcoded for testing
        return(false);

        /*boolean dateResult = false;
        Date curr = new Date();
        if (overallRuntimeMaxHours > 0) {
            long maxRuntime = overallRuntimeMaxHours * 60 * 60 * 1000;
            long currRuntime = curr.getTime() - this.start.getTime();
            dateResult = currRuntime > maxRuntime;
        }
        boolean itResult = false;
        if (overallIterationsMax > 0) {
            itResult = this.currIterations > this.overallIterationsMax;
        }
        return (dateResult || itResult); */
    }

    private void enqueueNewJobs(String[] initialJobs) {
        for (String msg : initialJobs) {
            try {
                System.out.println("SENDING JOB:\n '" + msg + "'" + this.jchannel);

                this.jchannel.basicPublish("", queueName + "_orders", MessageProperties.PERSISTENT_TEXT_PLAIN, msg.getBytes());
            } catch (IOException ex) {
                log.error(ex.toString());
            }
        }
    }

}
