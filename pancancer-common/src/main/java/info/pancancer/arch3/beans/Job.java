package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONObject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Job {


    private String state;
    private Utilities u = new Utilities();
    private String uuid = UUID.randomUUID().toString().toLowerCase();
    private String vmUuid;
    private String workflow;
    private String workflowVersion;
    private String workflowPath;
    private String jobHash;
    private Map<String, String> ini;
    private Timestamp createTs;
    private Timestamp updateTs;

    public Job(String workflow, String workflowVersion, String workflowPath, String jobHash, Map<String, String>  ini) {
        this.workflow = workflow;
        this.workflowVersion = workflowVersion;
        this.workflowPath = workflowPath;
        this.jobHash = jobHash;
        this.ini = ini;
    }

    public Job() {
        super();
    }

    public String toJSON () {

        StringBuffer j = new StringBuffer();
        j.append("{" +
        "\"message_type\": \"job\",\n" +
        "\"job_hash\": \""+jobHash+"\",\n" +
        "\"job_uuid\": \""+uuid+"\",\n" +
        "\"provision_uuid\": \""+vmUuid+"\",\n" +
        "\"workflow_name\": \""+workflow+"\",\n" +
        "\"workflow_version\" : \""+workflowVersion+"\",\n" +
        "\"workflow_path\" : \""+workflowPath+"\",\n" +
        "\"create_timestamp\" : \""+createTs+"\",\n" +
        "\"update_timestamp\" : \""+updateTs+"\",\n" +
        "\"arguments\" : {\n");

        boolean first = true;
        for (String key : ini.keySet()) {
            if (first) { first = false; } else { j.append(",\n"); }
            j.append("\""+key+"\": \""+ini.get(key)+"\"");
        }
        j.append("\n}\n");
        j.append("}\n");
        return(j.toString());
    }

    public Job fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        workflow = (String) obj.get("workflow_name");
        workflowVersion = (String) obj.get("workflow_version");
        workflowPath = (String) obj.get("workflow_path");
        jobHash = (String) obj.get("job_hash");
        uuid = (String) obj.get("job_uuid");
        vmUuid = (String) obj.get("provision_uuid");
        if (obj.get("create_timestamp") != null && !"null".equals((String)obj.get("create_timestamp"))) { createTs = Timestamp.valueOf ((String)obj.get("create_timestamp")); }
        if (obj.get("update_timestamp") != null && !"null".equals((String)obj.get("update_timestamp"))) { updateTs = Timestamp.valueOf ((String)obj.get("update_timestamp")); }
        JSONObject provision = (JSONObject) obj.get("arguments");
        ini = new HashMap<String, String>();
        for (Object key : provision.keySet()) {
            ini.put((String)key, (String)provision.get(key));
        }
        return(this);

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, String> getIni() {
        return ini;
    }

    public String getIniStr() {
        StringBuffer sb = new StringBuffer();
        for (String key : this.ini.keySet()) {
            sb.append(key+"="+this.ini.get(key));
        }
        return(sb.toString());
    }

    public void setIni(Map<String, String> ini) {
        this.ini = ini;
    }

    public String getJobHash() {
        return jobHash;
    }

    public void setJobHash(String jobHash) {
        this.jobHash = jobHash;
    }

    public String getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(String workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getWorkflowPath() {
        return workflowPath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }

    public Timestamp getCreateTs() {
        return createTs;
    }

    public void setCreateTs(Timestamp createTs) {
        this.createTs = createTs;
    }

    public Timestamp getUpdateTs() {
        return updateTs;
    }

    public void setUpdateTs(Timestamp updateTs) {
        this.updateTs = updateTs;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }
}
