package info.pancancer.arch3.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * A kitchen sink of utility methods, in a thread for some reason.
 *
 * @author boconnor
 */
public class Utilities /* extends Thread */{

    // message types
    public static final String VM_MESSAGE_TYPE = "vm-message-type";
    public static final String JOB_MESSAGE_TYPE = "job-message-type";

    private String outputFile = null;
    private ArrayList<JSONObject> resultsArr = new ArrayList<JSONObject>();

    public JSONObject parseJSONStr(String jsonStr) {
        JSONObject data = null;

        JSONParser parser = new JSONParser();
        try {
            data = (JSONObject) parser.parse(jsonStr);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

        return data;
    }

    public JSONObject parseConfig(String configFile) {
        String json = null;

        // local file specified on command line tried first
        File configFileObj = null;
        if (configFile != null) {
            configFileObj = new File(configFile);
        }
        File masterConfig = new File("/etc/genetic-algorithm/config.json");
        if (configFile != null && configFileObj.exists()) {
            System.out.println("USING CONFIG FROM SPECIFIED FILE!");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                json = sb.toString();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
                // Logger.getLogger(this.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else if (canDownloadConfig()) {
            System.out.println("USING CONFIG FROM USER DATA!");

            StringBuilder sb = new StringBuilder();
            URLConnection urlConn = null;
            InputStreamReader in = null;
            try {
                URL url = new URL("http://169.254.169.254/latest/user-data");
                urlConn = url.openConnection();
                if (urlConn != null) {
                    urlConn.setReadTimeout(MILLISECONDS_IN_A_MINUTE);
                }
                if (urlConn != null && urlConn.getInputStream() != null) {
                    in = new InputStreamReader(urlConn.getInputStream(), StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(in);
                    if (bufferedReader != null) {
                        int cp;
                        while ((cp = bufferedReader.read()) != -1) {
                            sb.append((char) cp);
                        }
                        bufferedReader.close();
                    }
                }
                in.close();
            } catch (Exception e) {
                throw new RuntimeException("Exception while calling URL: http://169.254.169.254/latest/user-data", e);
            }

            json = sb.toString();

        } else if (masterConfig.exists()) {
            System.out.println("USING CONFIG FROM /etc");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/etc/genetic-algorithm/config.json"),
                    StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                json = sb.toString();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        } else {
            return null;
        }

        return parseJSONStr(json);

    }

    public Channel setupQueue(JSONObject settings, String queue) {

        String server = (String) settings.get("rabbitMQHost");
        String user = (String) settings.get("rabbitMQUser");
        String pass = (String) settings.get("rabbitMQPass");

        Channel channel = null;

        try {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(server);
            factory.setUsername(user);
            factory.setPassword(pass);
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            channel.basicQos(1);
            channel.queueDeclare(queue, true, false, false, null);
            // channel.queueDeclarePassive(queue);

        } catch (Exception ex) {
            // Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
        }
        return (channel);

    }

    public Channel setupMultiQueue(JSONObject settings, String queue) {

        String server = (String) settings.get("rabbitMQHost");
        String user = (String) settings.get("rabbitMQUser");
        String pass = (String) settings.get("rabbitMQPass");

        Channel channel = null;

        try {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(server);
            factory.setUsername(user);
            factory.setPassword(pass);
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(queue, "fanout");

        } catch (Exception ex) {
            // Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
        }
        return (channel);

    }

    public JSONObject parseResult(String previous) {
        JSONObject obj = parseJSONStr(previous);
        resultsArr.add(obj);
        return (obj);
    }

    public JSONObject parseJob(String job) {
        return (parseJSONStr(job));
    }

    public ArrayList<JSONObject> getResultsArr() {
        return (resultsArr);
    }

    /**
     *
     * @param min
     *            The (included) lower bound of the range
     * @param max
     *            The (included) upper bound of the range
     *
     * @return The random value in the range
     */
    public static int randInRangeInc(int min, int max) {
        return min + (int) (Math.random() * ((1 + max) - min));
    }

    private boolean canDownloadConfig() {

        try {
            URL u = new URL("http://169.254.169.254/latest/user-data");
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("HEAD");
            if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder sb = new StringBuilder();
                URLConnection urlConn = null;
                InputStreamReader in = null;
                try {
                    URL url = new URL("http://169.254.169.254/latest/user-data");
                    urlConn = url.openConnection();
                    if (urlConn != null) {
                        urlConn.setReadTimeout(MILLISECONDS_IN_A_MINUTE);
                    }
                    if (urlConn != null && urlConn.getInputStream() != null) {
                        in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
                        BufferedReader bufferedReader = new BufferedReader(in);
                        if (bufferedReader != null) {
                            int cp;
                            while ((cp = bufferedReader.read()) != -1) {
                                sb.append((char) cp);
                            }
                            bufferedReader.close();
                        }
                    }
                    in.close();
                } catch (Exception e) {
                    throw new RuntimeException("Exception while calling URL: http://169.254.169.254/latest/user-data", e);
                }
                System.out.println("FROM USER DATA: " + sb.toString());
                System.out.println("MATCHES?: " + sb.toString().matches("^\\s*\\{\\.*\\}\\s*$"));
                return (sb.toString().matches("^\\s*\\{\\.*\\}\\s*$"));
            } else {
                return false;
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (false);
    }

    private static final int MILLISECONDS_IN_A_MINUTE = 60 * 1000;

    public String digest(String plaintext) {
        String result = null;
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            final int radix = 16;
            result = bigInt.toString(radix);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
}
