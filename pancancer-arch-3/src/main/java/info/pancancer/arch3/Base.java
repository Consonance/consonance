package info.pancancer.arch3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by boconnor on 15-04-18.
 */
public class Base {

    public static final int FIVE_SECOND_IN_MILLISECONDS = 5000;
    public static final int ONE_SECOND_IN_MILLISECONDS = 1000;
    public static final int DEFAULT_DISKSPACE = 1024;
    public static final int DEFAULT_MEMORY = 128;
    public static final int DEFAULT_NUM_CORES = 8;

    protected final Logger log = LoggerFactory.getLogger(getClass());

}
