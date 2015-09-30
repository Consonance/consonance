package io.consonance.arch.utils;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Utility methods for testing.
 *
 * @author dyuen
 */
public class ITUtilities {

    /**
     * Clears database state and known queues for testing.
     *
     * @throws IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public static void clearState() throws IOException, TimeoutException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        Utilities.clearState(parseConfig);
    }

}
