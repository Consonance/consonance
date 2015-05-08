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
package info.pancancer.arch3.utils;

import com.rabbitmq.client.Channel;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class UtilitiesIT {

    public static JSONObject getSettings() {
        File file = FileUtils.getFile("src", "test", "resources", "config.json");
        Utilities instance = new Utilities();
        JSONObject settings = instance.parseConfig(file.getAbsolutePath());
        return settings;
    }

    /**
     * Test of parseJSONStr method, of class Utilities.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testParseJSONStr() throws IOException {
        File file = FileUtils.getFile("src", "test", "resources", "config.json");
        Utilities instance = new Utilities();
        JSONObject result = instance.parseJSONStr(FileUtils.readFileToString(file));
        Assert.assertTrue("parsed json is invalid", !result.isEmpty());
    }

    /**
     * Test of setupQueue method, of class Utilities.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSetupQueue() throws IOException {
        Utilities instance = new Utilities();
        Channel result = instance.setupQueue(getSettings(), "testing_queue");
        assertTrue("could not open channel", result.isOpen());
        result.close();
    }

    /**
     * Test of setupMultiQueue method, of class Utilities.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testSetupMultiQueue() throws IOException {
        Utilities instance = new Utilities();
        Channel result = instance.setupMultiQueue(getSettings(), "testing_queue");
        assertTrue("could not open channel", result.isOpen());
        result.close();
    }

    /**
     * Test of randInRangeInc method, of class Utilities.
     */
    @Test
    public void testRandInRangeInc() {
        int min = 0;
        int max = 10;
        int result = Utilities.randInRangeInc(min, max);
        assertTrue("randomly generated value not in range", result >= min && result <= max);
    }

    /**
     * Test of digest method, of class Utilities.
     */
    @Test
    public void testDigest() {
        String plaintext = "";
        Utilities instance = new Utilities();
        String expResult = "d41d8cd98f00b204e9800998ecf8427e";
        String result = instance.digest(plaintext);
        assertEquals(expResult, result);
    }

}
