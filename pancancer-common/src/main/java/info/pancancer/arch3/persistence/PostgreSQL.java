package info.pancancer.arch3.persistence;

import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.Provision;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.KeyedHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by boconnor on 2015-04-22.
 */
public class PostgreSQL {

    protected static final Logger LOG = LoggerFactory.getLogger(PostgreSQL.class);
    private QueryRunner run = new QueryRunner();
    private String url;
    private Properties props;
    private PoolingDataSource<PoolableConnection> dataSource = null;

    public PostgreSQL(HierarchicalINIConfiguration settings) {
        try {
            String nullConfigs = "";
            String host = settings.getString(Constants.POSTGRES_HOST);
            if (host == null) {
                nullConfigs += "postgresHost ";
            }

            String user = settings.getString(Constants.POSTGRES_USERNAME);
            if (user == null) {
                nullConfigs += "postgresUser ";
            }

            String pass = settings.getString(Constants.POSTGRES_PASSWORD);
            if (pass == null) {
                nullConfigs += "postgresPass ";
            }

            String db = settings.getString(Constants.POSTGRES_DBNAME);
            if (db == null) {
                nullConfigs += "postgresDBName ";
            }

            String maxConnections = settings.getString(Constants.POSTGRES_MAX_CONNECTIONS, "5");

            if (nullConfigs.trim().length() > 0) {
                throw new NullPointerException("The following configuration values are null: " + nullConfigs
                        + ". Please check your configuration file.");
            }

            Class.forName("org.postgresql.Driver");

            this.url = "jdbc:postgresql://" + host + "/" + db;
            LOG.debug("PostgreSQL URL is: " + this.url);
            this.props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            // props.setProperty("ssl","true");
            props.setProperty("initialSize", "5");
            props.setProperty("maxActive", maxConnections);

            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(this.url, props);
            PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
            poolableConnectionFactory.setValidationQuery("select count(*) from job;");
            ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
            poolableConnectionFactory.setPool(connectionPool);
            dataSource = new PoolingDataSource<>(connectionPool);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public long getDesiredNumberOfVMs() {
        return runSelectStatement("select count(*) from provision where status = '" + ProvisionState.PENDING + "' or status = '"
                + ProvisionState.RUNNING + "'", new ScalarHandler<Long>());
    }

    public String getPendingProvisionUUID() {
        return runSelectStatement("select provision_uuid from provision where status = '" + ProvisionState.PENDING + "' limit 1",
                new ScalarHandler<String>());
    }

    public void clearDatabase() {
        this.runUpdateStatement("delete from provision; delete from job;");
    }

    private <T> T runSelectStatement(String query, ResultSetHandler<T> handler, Object... params) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
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
            conn = dataSource.getConnection();
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
            conn = dataSource.getConnection();
            run.update(conn, query, params);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    public void updatePendingProvision(String uuid) {
        runUpdateStatement("update provision set status = ?, update_timestamp = NOW() where provision_uuid = ?",
                ProvisionState.RUNNING.toString(), uuid);
    }

    public void finishContainer(String uuid) {
        runUpdateStatement("update provision set status = ? , update_timestamp = NOW() where provision_uuid = ? ",
                ProvisionState.SUCCESS.toString(), uuid);
    }

    public void updateJobMessage(String uuid, String stdout, String stderr) {
        runUpdateStatement("update job set stdout = ?, stderr = ?, update_timestamp = NOW() where job_uuid = ?", stdout, stderr, uuid);
    }

    public void finishJob(String uuid) {
        runUpdateStatement("update job set status = ? , update_timestamp = NOW() where job_uuid = ?", JobState.SUCCESS.toString(), uuid);
    }

    public void updateJob(String uuid, String vmUuid, JobState status) {
        runUpdateStatement("update job set status = ?, provision_uuid = ?, update_timestamp = NOW() where job_uuid = ?", status.toString(),
                vmUuid, uuid);
    }

    public void updateProvisionByProvisionUUID(String provisionUuid, String jobUuid, ProvisionState status, String ipAddress) {
        runUpdateStatement(
                "update provision set status = ? , job_uuid = ? , update_timestamp = NOW(), ip_address = ? where provision_uuid = ?",
                status.toString(), jobUuid, ipAddress, provisionUuid);
    }

    public void updateProvisionByJobUUID(String jobUUID, String provisionUUID, ProvisionState status, String ipAddress) {
        runUpdateStatement(
                "update provision set status = ? , provision_uuid = ?, update_timestamp = NOW(), ip_address = ? where job_uuid = ?",
                status.toString(), provisionUUID, ipAddress, jobUUID);
    }

    public long getProvisionCount(ProvisionState status) {
        return this.runSelectStatement("select count(*) from provision where status = ?", new ScalarHandler<Long>(), status.toString());
    }

    public Integer createProvision(Provision p) {
        Map<Object, Map<String, Object>> map = this.runInsertStatement(
                "INSERT INTO provision (status, provision_uuid, cores, mem_gb, storage_gb, job_uuid, ip_address) VALUES (?,?,?,?,?,?,?)",
                new KeyedHandler<>("provision_id"), p.getState().toString(), p.getProvisionUUID(), p.getCores(), p.getMemGb(),
                p.getStorageGb(), p.getJobUUID(), p.getIpAddress());
        return (Integer) map.entrySet().iterator().next().getKey();
    }

    public String createJob(Job j) {
        JSONObject jsonIni = new JSONObject(j.getIni());
        Map<Object, Map<String, Object>> map = this.runInsertStatement(
                "INSERT INTO job (status, job_uuid, workflow, workflow_version, job_hash, ini) VALUES (?,?,?,?,?,?)", new KeyedHandler<>(
                        "job_uuid"), j.getState().toString(), j.getUuid(), j.getWorkflow(), j.getWorkflowVersion(), j.getJobHash(), jsonIni
                        .toJSONString());
        return (String) map.entrySet().iterator().next().getKey();
    }

    public String[] getSuccessfulVMAddresses() {
        Map<String, Map<String, Object>> runSelectStatement = runSelectStatement(
                "select provision_id, ip_address from provision where status = '" + ProvisionState.SUCCESS + "'", new KeyedHandler<String>(
                        "provision_id"));
        List<String> list = new ArrayList<>();
        for (Entry<String, Map<String, Object>> entry : runSelectStatement.entrySet()) {
            list.add((String) entry.getValue().get("ip_address"));
        }
        return list.toArray(new String[list.size()]);
    }

    public List<Job> getJobs(JobState status) {

        List<Job> jobs = new ArrayList<>();
        Map<Object, Map<String, Object>> map;
        if (status != null) {
            map = this.runSelectStatement("select * from job where status = ?", new KeyedHandler<>("job_uuid"), status.toString());
        } else {
            map = this.runSelectStatement("select * from job", new KeyedHandler<>("job_uuid"));
        }

        for (Entry<Object, Map<String, Object>> entry : map.entrySet()) {

            Job j = new Job();
            j.setState(Enum.valueOf(JobState.class, (String) entry.getValue().get("status")));
            j.setUuid((String) entry.getValue().get("job_uuid"));
            j.setWorkflow((String) entry.getValue().get("workflow"));
            j.setWorkflowVersion((String) entry.getValue().get("workflow_version"));
            j.setJobHash((String) entry.getValue().get("job_hash"));
            j.setStdout((String) entry.getValue().get("stdout"));
            j.setStderr((String) entry.getValue().get("stderr"));
            JSONObject iniJson = Utilities.parseJSONStr((String) entry.getValue().get("ini"));
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
