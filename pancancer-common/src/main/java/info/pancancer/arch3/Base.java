package info.pancancer.arch3;

import java.io.IOException;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by boconnor on 15-04-18.
 */
public class Base {

    public static final int FIVE_SECOND_IN_MILLISECONDS = 5000;
    public static final int ONE_SECOND_IN_MILLISECONDS = 1000;
    public static final long TEN_MINUTES_IN_MILLISECONDS = 600000;
    public static final long ONE_MINUTE_IN_MILLISECONDS = 60000;
    public static final int DEFAULT_DISKSPACE = 1024;
    public static final int DEFAULT_MEMORY = 128;
    public static final int DEFAULT_NUM_CORES = 8;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected OptionParser parser;
    protected ArgumentAcceptingOptionSpec<String> configSpec;
    protected OptionSpecBuilder endlessSpec;
    protected String configFile;
    protected OptionSet options;

    public Base() {
        this.parser = new OptionParser();
        this.configSpec = parser.accepts("config", "specify a config file").withOptionalArg().ofType(String.class)
                .describedAs("path to json config").required();
        this.endlessSpec = parser.accepts("endless", "run this endlessly");
    }

    public void parseOptions(String[] argv) throws IOException {
        try {
            this.options = parser.parse(argv);
        } catch (OptionException ex) {
            parser.printHelpOn(System.out);
            throw ex;
        }

        this.configFile = null;
        if (options.has(configSpec)) {
            this.configFile = options.valueOf(configSpec);
        }
    }

}
