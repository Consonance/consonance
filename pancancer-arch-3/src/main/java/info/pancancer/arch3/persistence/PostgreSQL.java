package info.pancancer.arch3.persistence;

import info.pancancer.arch3.Base;
import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.utils.Utilities;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.KeyedHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.json.simple.JSONObject;

/**
 * Created by boconnor on 2015-04-22.
 */
public class PostgreSQL extends Base {

    QueryRunner run = new QueryRunner();
    private String url;
    private Properties props;

    public PostgreSQL(JSONObject settings) {

        try {
            String host = (String) settings.get("postgresHost");
            String user = (String) settings.get("postgresUser");
            String pass = (String) settings.get("postgresPass");
            String db = (String) settings.get("postgresDBName");

            Class.forName("org.postgresql.Driver");

            this.url = "jdbc:postgresql://" + host + "/" + db;
            this.props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            // props.setProperty("ssl","true");

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public String getPendingProvisionUUID() {
        return runSelectStatement("select provision_uuid from provision where status = 'pending' limit 1", new ScalarHandler<String>(),
                false);
    }

    private <T> T runSelectStatement(String query, ResultSetHandler<T> handler, Object... params) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, props);
            return run.query(conn, query, handler, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private <T> T runInsertStatement(String query, ResultSetHandler<T> handler, Object... params) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, props);
            return run.insert(conn, query, handler, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private boolean runUpdateStatement(String query, Object... params) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, props);
            run.update(conn, query, params);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    public void updatePendingProvision(String uuid) {
        runUpdateStatement("update provision set status = ?, update_timestamp = NOW() where provision_uuid = ?", Utilities.RUNNING, uuid);
    }

    public void finishContainer(String uuid) {
        runUpdateStatement("update provision set status = ? , update_timestamp = NOW() where provision_uuid = ? ", Utilities.SUCCESS, uuid);
    }

    public void finishJob(String uuid) {
        runUpdateStatement("update job set status = ? , update_timestamp = NOW() where job_uuid = ?", Utilities.SUCCESS, uuid);
    }

    public void updateJob(String uuid, String vmUuid, String status) {
        runUpdateStatement("update job set status = ?, provision_uuid = ?, update_timestamp = NOW() where job_uuid = ?", status, vmUuid,
                uuid);
    }

    public void updateProvision(String uuid, String jobUuid, String status) {
        runUpdateStatement("update provision set status = ? , job_uuid = ? , update_timestamp = NOW() where provision_uuid = ?", status,
                jobUuid, uuid);
    }

    public int getProvisionCount(String status) {
        return this.runSelectStatement("select count(*) from provision where status = ?", new ScalarHandler<Integer>(), false, status);
    }

    public String createProvision(Provision p) {
        Map<Object, Map<String, Object>> map = this.runInsertStatement(
                "INSERT INTO provision (status, provision_uuid, cores, mem_gb, storage_gb) VALUES (?,?,?,?,?)", new KeyedHandler<>(
                        "provision_uuid"), p.getState(), p.getUuid(), p.getCores(), p.getMemGb(), p.getStorageGb());
        return (String) map.entrySet().iterator().next().getKey();
    }

    public String createJob(Job j) {
        JSONObject jsonIni = new JSONObject(j.getIni());
        Map<Object, Map<String, Object>> map = this.runInsertStatement(
                "INSERT INTO job (status, job_uuid, workflow, workflow_version, job_hash, ini) VALUES (?,?,?,?,?,?)", new KeyedHandler<>(
                        "job_uuid"), j.getState(), j.getUuid(), j.getWorkflow(), j.getWorkflowVersion(), j.getJobHash(), jsonIni
                        .toJSONString());
        return (String) map.entrySet().iterator().next().getKey();
    }

    public List<Job> getJobs(String status) {

        List<Job> jobs = new ArrayList<>();
        Map<Object, Map<String, Object>> map;
        if (status != null && !"".equals(status)) {
            map = this.runSelectStatement("select * from job where status = ?", new KeyedHandler<>("job_uuid"), status);
        } else {
            map = this.runSelectStatement("select * from job", new KeyedHandler<>("job_uuid"));
        }

        Utilities u = new Utilities();

        for (Entry<Object, Map<String, Object>> entry : map.entrySet()) {

            Job j = new Job();
            j.setState((String) entry.getValue().get("status"));
            j.setUuid((String) entry.getValue().get("job_uuid"));
            j.setWorkflow((String) entry.getValue().get("workflow"));
            j.setWorkflowVersion((String) entry.getValue().get("workflow_version"));
            j.setJobHash((String) entry.getValue().get("job_hash"));
            JSONObject iniJson = u.parseJSONStr((String) entry.getValue().get("ini"));
            HashMap<String, String> ini = new HashMap<>();
            for (Object key : iniJson.keySet()) {
                ini.put((String) key, (String) iniJson.get(key));
            }
            j.setIni(ini);

            // timestamp
            Timestamp createTs = (Timestamp) entry.getValue().get("create_timestamp");
            Timestamp updateTs = (Timestamp) entry.getValue().get("update_timestamp");
            j.setCreateTs(createTs);
            j.setUpdateTs(updateTs);

            jobs.add(j);

        }

        return jobs;
    }

    public boolean previouslyRun(String hash) {
        Object[] runSelectStatement = this.runSelectStatement("select * from job where job_hash = ?", new ArrayHandler(), hash);
        return (runSelectStatement.length > 0);
    }

}
