package io.consonance.common;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import java.io.File;

/**
 * @author dyuen
 */
public class Utilities {

    public static HierarchicalINIConfiguration getYouxiaConfig() {
        File configFile = new File(System.getProperty("user.home"), ".consonance/config");
        return Utilities.parseConfig(configFile.getAbsolutePath());
    }

    public static HierarchicalINIConfiguration parseConfig(String path) {
        try {
            return new HierarchicalINIConfiguration(path);
        } catch (ConfigurationException ex) {
            throw new RuntimeException("Could not read ~/.consonance/config");
        }
    }
}
