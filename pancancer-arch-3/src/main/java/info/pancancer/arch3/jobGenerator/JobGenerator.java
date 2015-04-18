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

/**
 * Created by boconnor on 15-04-18.
 *
 * takes a --config option pointed to a config .json file
 */
public class JobGenerator {

    public JobGenerator(String configFile) {
        
    }

    public static void main(String [] args)
    {
        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        if (options.has("config")) { configFile = (String)options.valueOf("config"); }

        JobGenerator jg = new JobGenerator(configFile);
        System.out.println("MASTER FINISHED, EXITING!");
    }

}
