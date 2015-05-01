package info.pancancer.arch3.persistence;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by boconnor on 2015-04-22.
 */
public class PostgreSQL extends Base {

    Connection conn = null;

    public PostgreSQL(JSONObject settings) {

        try {
            String host = (String) settings.get("postgresHost");
            String user = (String) settings.get("postgresUser");
            String pass = (String) settings.get("postgresPass");
            String db = (String) settings.get("postgresDBName");

            Class.forName("org.postgresql.Driver");

            String url = "jdbc:postgresql://"+host+"/"+db;
            Properties props = new Properties();
            props.setProperty("user",user);
            props.setProperty("password",pass);
            //props.setProperty("ssl","true");
            conn = DriverManager.getConnection(url, props);

            log.debug("test "+conn);

        } catch (SQLException e) {
            log.error(e.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void close () {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error(e.toString());
        }
    }

    public String getPendingProvisionUUID() {

        String uuid = null;

        try {

            Statement stmt = conn.createStatement();

            String sql = "select provision_uuid from provision where status = 'pending' limit 1";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                uuid = rs.getString(1);
            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.toString());
        }

        return(uuid);
    }

    public void updatePendingProvision(String uuid) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update provision set status = '"+Utilities.RUNNING+"', update_timestamp = NOW() where provision_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finishContainer(String uuid) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update provision set status = '"+ Utilities.SUCCESS+"', update_timestamp = NOW() where provision_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finishJob(String uuid) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update job set status = '"+ Utilities.SUCCESS+"', update_timestamp = NOW() where job_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateJob(String uuid, String vmUuid, String status) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update job set status = '"+ status +"', provision_uuid = '"+vmUuid+"', update_timestamp = NOW() where job_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateProvision(String uuid, String jobUuid, String status) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update provision set status = '"+ status +"', job_uuid = '"+jobUuid+"', update_timestamp = NOW() where provision_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getProvisionCount(String status) {

        int count = 0;

        try {

            Statement stmt = conn.createStatement();

            String sql = "select count(*) from provision where status = '"+status+"'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return(count);
    }

    public String createProvision (Provision p) {

        try {

            Statement stmt = conn.createStatement();

            String sql = "INSERT INTO provision " +
                    "(status, provision_uuid, cores, mem_gb, storage_gb) " +
                    "VALUES ('"+p.getState()+"'," +
                    " '"+p.getUuid()+"'," +
                    " "+p.getCores()+"," +
                    " "+p.getMemGb()+"," +
                    " "+p.getStorageGb() +
                    " )";
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return(p.getUuid());

    }

    public String createJob (Job j) {
        try {

            Statement stmt = conn.createStatement();

            // prepare INI JSON
            JSONObject jsonIni = new JSONObject(j.getIni());

            String sql = "INSERT INTO job " +
                    "(status, job_uuid, workflow, workflow_version, job_hash, ini) " +
                    "VALUES ('"+j.getState()+"'," +
                    " '"+j.getUuid()+"'," +
                    " '"+j.getWorkflow()+"'," +
                    " '"+j.getWorkflowVersion()+"'," +
                    " '"+ j.getJobHash() + "', " +
                    " '"+ jsonIni.toJSONString() + "'" +
                    " )";
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return(j.getUuid());
    }

    public List<Job> getJobs(String status) {

        ArrayList<Job> jobs = new ArrayList<Job>();

        try {

            Statement stmt = conn.createStatement();
            Utilities u = new Utilities();

            String sql = "select * from job";
            if (status != null && !"".equals(status)) { sql = "select * from job where status = '"+status+"'"; }
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {

                Job j = new Job();
                j.setState(rs.getString("status"));
                j.setUuid(rs.getString("job_uuid"));
                j.setWorkflow(rs.getString("workflow"));
                j.setWorkflowVersion(rs.getString("workflow_version"));
                j.setJobHash(rs.getString("job_hash"));
                JSONObject iniJson = u.parseJSONStr(rs.getString("ini"));
                HashMap<String, String> ini = new HashMap<String, String>();
                for (Object key : iniJson.keySet()) {
                    ini.put((String)key, (String)iniJson.get(key));
                }
                j.setIni(ini);

                // timestamp
                Timestamp createTs = rs.getTimestamp("create_timestamp");
                Timestamp updateTs = rs.getTimestamp("update_timestamp");
                j.setCreateTs(createTs);
                j.setUpdateTs(updateTs);

                jobs.add(j);

            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.toString());
        }

        return(jobs);
    }

    public boolean previouslyRun(String hash) {

        boolean seen = false;

        try {
            Statement stmt = conn.createStatement();
            String sql = "select * from job where job_hash = '"+hash+"'";
            ResultSet rs = stmt.executeQuery(sql);
            return(rs.next());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return(seen);
    }

}
