/*
 * Copyright (C) 2015 CancerCollaboratory
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.consonance.arch.utils;

import com.rabbitmq.client.Channel;
import io.consonance.common.CommonTestUtilities;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author dyuen
 */
public class UtilitiesIT {

    public static HierarchicalINIConfiguration getSettings() {
        File file = FileUtils.getFile("src", "test", "resources", "config");
        CommonServerTestUtilities instance = new CommonServerTestUtilities();
        HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(file.getAbsolutePath());
        return settings;
    }

    /**
     * Test of parseJSONStr method, of class CommonServerTestUtilities.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testParseJSONStr() throws IOException {
        File file = FileUtils.getFile("src", "test", "resources", "config.json");
        CommonServerTestUtilities instance = new CommonServerTestUtilities();
        JSONObject result = CommonServerTestUtilities.parseJSONStr(FileUtils.readFileToString(file));
        Assert.assertTrue("parsed json is invalid", !result.isEmpty());
    }

    /**
     * Test of setupQueue method, of class CommonServerTestUtilities.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSetupQueue() throws IOException, TimeoutException {
        CommonServerTestUtilities instance = new CommonServerTestUtilities();
        Channel result = CommonServerTestUtilities.setupQueue(getSettings(), "testing_queue");
        assertTrue("could not open channel", result.isOpen());
        result.close();
    }

    /**
     * Test of setupExchange method, of class CommonServerTestUtilities.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSetupMultiQueue() throws IOException, TimeoutException {
        CommonServerTestUtilities instance = new CommonServerTestUtilities();
        Channel result = CommonServerTestUtilities.setupExchange(getSettings(), "testing_queue");
        assertTrue("could not open channel", result.isOpen());
        result.close();
    }

    /**
     * Test of randInRangeInc method, of class CommonServerTestUtilities.
     */
    @Test
    public void testRandInRangeInc() {
        int min = 0;
        int max = 10;
        int result = CommonServerTestUtilities.randInRangeInc(min, max);
        assertTrue("randomly generated value not in range", result >= min && result <= max);
    }


}
