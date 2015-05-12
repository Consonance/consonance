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
package info.pancancer.arch3.coordinator;

import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import joptsimple.OptionException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This may be a dead class.
 * 
 * @author dyuen
 */
public class CoordinatorResultIT {

    @BeforeClass
    public static void setup() throws IOException {
        Utilities.clearState();
    }

    /**
     * Test of main method, of class CoordinatorResult.
     */
    @Test(expected = OptionException.class)
    public void testMainUsage() throws Exception {
        CoordinatorResult.main(new String[] { "--help" });
    }

}
