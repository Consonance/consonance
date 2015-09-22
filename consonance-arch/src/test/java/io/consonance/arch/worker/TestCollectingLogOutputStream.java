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
package io.consonance.arch.worker;

import java.util.List;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author dyuen
 */
public class TestCollectingLogOutputStream {

    /**
     * Test of processLine method, of class CollectingLogOutputStream.
     */
    @Test
    public void testProcessLine() {
        CollectingLogOutputStream instance = new CollectingLogOutputStream();
        instance.processLine("add a line of output", 1);
    }

    /**
     * Test of getAllLinesAsString method, of class CollectingLogOutputStream.
     */
    @Test
    public void testGetAllLinesAsString() {
        CollectingLogOutputStream instance = new CollectingLogOutputStream();
        instance.processLine("add a line of funky output", 1);
        instance.processLine("add another line of groovy output", 2);
        String output = instance.getAllLinesAsString();
        assertTrue("output does not match", output.contains("funky") && output.contains("groovy"));
    }

    /**
     * Test of getLastNLines method, of class CollectingLogOutputStream.
     */
    @Test
    public void testGetLastNLines() {
        CollectingLogOutputStream instance = new CollectingLogOutputStream();
        instance.processLine("add a line of funky output", 1);
        instance.processLine("add another line of groovy output", 2);
        List<String> output = instance.getLastNLines(1);
        assertTrue("output does not match", output.size() == 1 && output.get(0).contains("groovy"));
    }

}
