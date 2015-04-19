package info.pancancer.arch3.jobGenerator;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
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

            // SETUP QUEUE
            this.jchannel = u.setupQueue(settings, queueName + "_jobs");
            this.rchannel = u.setupQueue(settings, queueName + "_results");

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
            int messages = jchannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
            System.out.println("JOB QUEUE SIZE: " + messages);
            // if there are no messages then we'll want to add some new jobs
            if (messages < 10 && !exceededTimeOrJobs()) {
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
        return ("{\n"
          + "      \"command\" : \"echo foo\"\n"
          + "    }");
    }

    private boolean exceededTimeOrJobs() {
        boolean dateResult = false;
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
        return (dateResult || itResult);
    }

    private void enqueueNewJobs(String[] initialJobs) {
        for (String msg : initialJobs) {
            try {
                System.out.println("SENDING JOB:\n '" + msg + "'" + this.jchannel);

                this.jchannel.basicPublish("", queueName + "_jobs", MessageProperties.PERSISTENT_TEXT_PLAIN, msg.getBytes());
            } catch (IOException ex) {
                log.error(ex.toString());
            }
        }
    }

}
