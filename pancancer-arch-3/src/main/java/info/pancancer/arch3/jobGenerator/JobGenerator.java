package info.pancancer.arch3.jobGenerator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;

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
    private Connection connection = null;
    private String queueName = null;
    private ArrayList<JSONObject> resultsArr = null;
    private QueueingConsumer rconsumer = null;
    private Date start = new Date();
    private int currIterations = 0;
    private int overallIterationsMax = 0;
    private int overallRuntimeMaxHours = 0;
    private int totalJobs = 1;

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        parser.accepts("total-jobs").withOptionalArg().ofType(Integer.class);
        OptionSet options = parser.parse(args);

        String configFile = null;
        if (options.has("config")) {
            configFile = (String) options.valueOf("config");
        }
        int totalJobs = 1;
        if (options.has("total-jobs")) {
            totalJobs = (Integer) options.valueOf("total-jobs");
        }

        JobGenerator jg = new JobGenerator(configFile, totalJobs);

        jg.log.info("MASTER FINISHED, EXITING!");
    }

    public JobGenerator(String configFile, int totalJobs) {

        // UTILS OBJECT
        Utilities u = new Utilities();
        settings = u.parseConfig(configFile);
        if (outputFile == null) {
            outputFile = (String) settings.get("results");
        }
        u.setupOutputFile(outputFile, settings);
        overallRuntimeMaxHours = ((Number) settings.get("overallRuntimeMaxHours")).intValue();
        overallIterationsMax = ((Number) settings.get("overallIterationsMax")).intValue();
        // Utilities will handle persisting data to disk on exit
        Runtime.getRuntime().addShutdownHook(u);
        resultsArr = u.getResultsArr();

        // Params
        this.totalJobs = totalJobs;

        // CONFIG
        queueName = (String) settings.get("rabbitMQQueueName");
        System.out.println("QUEUE NAME: " + queueName);

        // SETUP QUEUE
        this.jchannel = u.setupQueue(settings, queueName + "_orders");

        // LOOP, ADDING JOBS EVERY FEW MINUTES
        while (totalJobs > 0) {

            totalJobs--;

            // keep track of the iterations
            currIterations++;

            System.out.println("\nGENERATING NEW JOBS\n");
            // TODO: this is fake, in a real program this is being read from JSONL file or web service
            // check to see if new results are available and/or if the work queue is empty
            String[] newJobs = generateNewJobs("", resultsArr, u);

            // enqueue new jobs if we have them
            if (newJobs.length > 0) {
                enqueueNewJobs(newJobs);
            } else {
                // System.out.println("CAN'T FIND NEW STATE TO TRY, LIKELY CONVERGED");
                totalJobs = 0;
            }

            // decide to exit
            if (exceededTimeOrJobs()) {
                totalJobs = 0;
                // System.out.println("TIME OR JOBS EXCEEDED, EXITING");
            } else {
                try {
                    // pause
                    Thread.sleep(Base.ONE_SECOND_IN_MILLISECONDS);
                } catch (InterruptedException ex) {
                    log.error(ex.toString());
                }
            }
        }

        try {

            jchannel.getConnection().close(Base.FIVE_SECOND_IN_MILLISECONDS);

        } catch (IOException ex) {
            log.error(ex.toString());
        }

    }

    // PRIVATE

    private String[] generateNewJobs(String baseCmd, ArrayList<JSONObject> resultsArr, Utilities u) {
        ArrayList<String> jobs = new ArrayList<>();
        try {
            int messages = jchannel.queueDeclarePassive(queueName + "_orders").getMessageCount();
            System.out.println("JOB QUEUE SIZE: " + messages);
            // if there are no messages then we'll want to add some new jobs
            if (!exceededTimeOrJobs()) {
                // TODO, actually generate new jobs if the job queue is empty
                Order newOrder = makeNewOrder(baseCmd, resultsArr, u);
                if (newOrder != null) {
                    jobs.add(newOrder.toJSON());
                }
            }

        } catch (IOException ex) {
            log.error(ex.toString());
        }
        return jobs.toArray(new String[0]);

    }

    // TODO: this will actually need to come from a file or web service
    private Order makeNewOrder(String baseCmd, ArrayList<JSONObject> resultsArr, Utilities u) {

        // TODO: will need to make this from parameters in INI
        String uuid = UUID.randomUUID().toString().toLowerCase();
        String hashStr = u.digest(uuid);

        // TODO: this will come from a web service or file
        HashMap<String, String> hm = new HashMap<>();
        hm.put("param1", "bar");
        hm.put("param2", "foo");

        int cores = Base.DEFAULT_NUM_CORES;
        int memGb = Base.DEFAULT_MEMORY;
        int storageGb = Base.DEFAULT_DISKSPACE;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");

        Order newOrder = new Order();
        newOrder.setJob(new Job("DEWrapperWorkflow", "1.0.0", "/path/to/workflow", hashStr, hm));
        newOrder.setProvision(new Provision(cores, memGb, storageGb, a));

        return newOrder;

    }

    private boolean exceededTimeOrJobs() {

        // FIXME: hardcoded for testing
        return false;
        /*
         * boolean dateResult = false; Date curr = new Date(); if (overallRuntimeMaxHours > 0) { long maxRuntime = overallRuntimeMaxHours *
         * 60 * 60 * 1000; long currRuntime = curr.getTime() - this.start.getTime(); dateResult = currRuntime > maxRuntime; } boolean
         * itResult = false; if (overallIterationsMax > 0) { itResult = this.currIterations > this.overallIterationsMax; } return
         * (dateResult || itResult);
         */
    }

    private void enqueueNewJobs(String[] initialJobs) {
        for (String msg : initialJobs) {
            try {
                System.out.println("\nSENDING JOB:\n '" + msg + "'\n" + this.jchannel + " \n");

                this.jchannel.basicPublish("", queueName + "_orders", MessageProperties.PERSISTENT_TEXT_PLAIN,
                        msg.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                log.error(ex.toString());
            }
        }
    }

}
