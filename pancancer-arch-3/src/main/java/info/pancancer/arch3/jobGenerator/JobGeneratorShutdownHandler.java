package info.pancancer.arch3.jobGenerator;

import info.pancancer.arch3.utils.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class JobGeneratorShutdownHandler extends Thread {
    private String outputFile = null;
    private ArrayList<JSONObject> resultsArr = new ArrayList<JSONObject>();
    private static final CharsetEncoder ENCODER = StandardCharsets.UTF_8.newEncoder();

    /**
     * This is here to exclusively do cleanup after a cntl+c e.g. persist to disk
     */
    @Override
    public void run() {
        if (outputFile != null) {
            JSONObject obj = new JSONObject();
            obj.put("results", resultsArr);
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), ENCODER));
                obj.writeJSONString(bw);
                bw.close();
                System.out.println("WRITING RESULTS TO " + this.outputFile);
            } catch (IOException ex) {
                Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setupOutputFile(String outputFile, JSONObject settings) {
        this.outputFile = outputFile;
        if (this.outputFile == null) {
            this.outputFile = (String) settings.get("results");
        }
        try {
            File existing = new File(this.outputFile);
            if (existing.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile), ENCODER.charset()));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                String json = sb.toString();
                br.close();
                Utilities u = new Utilities();
                JSONObject parsed = u.parseJSONStr(json);
                resultsArr = (ArrayList<JSONObject>) parsed.get("results");

            }
        } catch (Exception ex) {
            Logger.getLogger(JobGeneratorShutdownHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
