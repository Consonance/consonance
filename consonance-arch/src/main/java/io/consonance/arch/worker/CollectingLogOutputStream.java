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

package io.consonance.arch.worker;

import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A log output stream to use with Apache Exec.
 *
 * @author sshorser
 *
 */
public class CollectingLogOutputStream extends LogOutputStream {
    private final List<String> lines = new LinkedList<>();

    /**
     * Process a line.
     * 
     * @param line
     *            - A line.
     * @param level
     *            - a logging level. Not used in this implementation.
     */
    @Override
    protected void processLine(String line, int level) {
        Lock lock = new ReentrantLock();
        lock.lock();
        // workaround for dcc-storage, break up on carriage returns as well
        String[] splitLines = line.split("\r");
        for(String lineSegment: splitLines){
            lines.add(lineSegment);
        }
        lock.unlock();
    }

    /**
     * Get all the lines concatenated into a single string, with \n between each line.
     * 
     * @return
     */
    public String getAllLinesAsString() {
        Lock lock = new ReentrantLock();
        lock.lock();
        // TODO: Add functionality to allow other join characters besides \n ? (not urgent)
        String allTheLines = StringUtils.join(this.lines, "\n");
        lock.unlock();
        return allTheLines;
    }

    /**
     * Get the last *n* lines in the log.
     * 
     * @param n
     *            - The number of lines to get.
     * @return A list of strings.
     */
    public List<String> getLastNLines(int n) {
        Lock lock = new ReentrantLock();
        lock.lock();
        List<String> nlines = new ArrayList<String>(n);
        int start, end;
        end = this.lines.size();
        start = Math.max(0, this.lines.size() - n);
        if (end > start && start >= 0) {
            nlines = this.lines.subList(start, end);
        }
        lock.unlock();
        return nlines;
    }
}
