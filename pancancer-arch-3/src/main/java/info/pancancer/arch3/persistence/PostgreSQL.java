package info.pancancer.arch3.persistence;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Order;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONObject;

import java.sql.*;
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
        }

        return(uuid);
    }

    public void updatePendingProvision(String uuid) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update provision set status = '"+Utilities.RUNNING+"' where provision_id in (select provision_id from provision where status = '"+Utilities.PENDING+"' and provision_uuid = '"+uuid+"')";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finishContainer(String uuid) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update provision set status = '"+ Utilities.SUCCESS+"' where provision_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finishJob(String uuid) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update job set status = '"+ Utilities.SUCCESS+"' where job_uuid = '"+uuid+"'";
            stmt.execute(sql);
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateJob(String uuid, String status) {
        try {

            Statement stmt = conn.createStatement();

            String sql = "update job set status = '"+ status +"' where job_uuid = '"+uuid+"'";
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

            String sql = "INSERT INTO job " +
                    "(status, job_uuid, workflow, workflow_version, job_hash, ini) " +
                    "VALUES ('"+j.getState()+"'," +
                    " '"+j.getUuid()+"'," +
                    " '"+j.getWorkflow()+"'," +
                    " '"+j.getWorkflowVersion()+"'," +
                    " '"+ j.getJobHash() + "', " +
                    " '"+ j.getIni() + "'" +
                    " )";
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return(j.getUuid());
    }

}
