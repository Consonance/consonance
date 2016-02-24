/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.consonance.arch;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author boconnor
 * @author dyuen
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
