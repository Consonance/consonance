package info.pancancer.arch3.jobGenerator;

import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobGeneratorShutdownHandler extends Thread {
    private String outputFile = null;
    protected static final Logger LOG = LoggerFactory.getLogger(JobGeneratorShutdownHandler.class);
    private ArrayList<JSONObject> resultsArr = new ArrayList<JSONObject>();

    /**
     * This is here to exclusively do cleanup after a cntl+c e.g. persist to disk
     */
    @Override
    public void run() {
        if (outputFile != null) {
            JSONObject obj = new JSONObject();
            obj.put("results", resultsArr);
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
                obj.writeJSONString(bw);
                bw.close();
                LOG.info("WRITING RESULTS TO " + this.outputFile);
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
    }

    public void setupOutputFile(String outputFile, HierarchicalINIConfiguration settings) {
        this.outputFile = outputFile;
        if (this.outputFile == null) {
            this.outputFile = settings.getString(Constants.JOB_GENERATOR_RESULTS_FILE);
        }
        try {
            File existing = new File(this.outputFile);
            if (existing.exists()) {
                String json;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();
                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                    json = sb.toString();
                }
                // Utilities u = new Utilities();
                JSONObject parsed = Utilities.parseJSONStr(json);
                resultsArr = (ArrayList<JSONObject>) parsed.get("results");

            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
