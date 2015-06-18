package info.pancancer.arch3.worker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.lang3.StringUtils;

/**
 * A log output stream to use with Apache Exec.
 * 
 * @author sshorser
 *
 */
public class CollectingLogOutputStream extends LogOutputStream {
    private final List<String> lines = new LinkedList<String>();

    
    /**
     * Process a line.
     * @param line - A line.
     * @param level - a logging level. Not used in this implementation.
     */
    @Override
    protected void processLine(String line, int level) {
        Lock lock = new ReentrantLock();
        lock.lock();
        lines.add(line);
        lock.unlock();
    }

    /**
     * Get all the lines concatenated into a single string, with \n between each line.
     * @return
     */
    public String getAllLinesAsString() {
        Lock lock = new ReentrantLock();
        lock.lock();
        //TODO: Add functionality to allow other join characters besides \n ? (not urgent)
        String allTheLines = StringUtils.join(this.lines, "\n");
        lock.unlock();
        return allTheLines;
    }

    /**
     * Get the last *n* lines in the log.
     * @param n - The number of lines to get. 
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
