package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Provision {

    long cores;
    long memGb;
    long storageGb;
    String state;
    List<String> ansiblePlaybooks;
    Utilities u = new Utilities();
    String uuid = UUID.randomUUID().toString().toLowerCase();

    public Provision(int cores, int memGb, int storageGb, List<String> ansiblePlaybooks) {
        this.cores = cores;
        this.memGb = memGb;
        this.storageGb = storageGb;
        this.ansiblePlaybooks = ansiblePlaybooks;
    }

    public Provision() {
        super();
    }


    public String toJSON () {

        StringBuffer j = new StringBuffer();

        j.append("{" +
            "   \"message_type\": \"provision\",\n" +
                "\"provision_uuid\": \""+uuid+"\",\n" +
            "   \"cores\": "+cores+",\n" +
            "    \"mem_gb\": "+memGb+",\n" +
            "    \"storage_gb\": "+storageGb+",\n" +
            "    \"bindle_profiles_to_run\": [");

        boolean first = true;
        for (String playbook : ansiblePlaybooks) {
            if (first) { first = false; } else { j.append(",\n"); }
            j.append("\""+playbook+"\"");
        }
        j.append("\n]\n}\n");
        return (j.toString());
    }

    public Provision fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        cores = (Long) obj.get("cores");
        memGb = (Long) obj.get("mem_gb");
        storageGb = (Long) obj.get("storage_gb");
        uuid = (String) obj.get("provision_uuid");
        JSONArray playbooks = (JSONArray) obj.get("bindle_profiles_to_run");
        ansiblePlaybooks = new ArrayList<String>();
        for (Object key : playbooks) {
            ansiblePlaybooks.add((String)key);
        }
        return(this);

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(long storageGb) {
        this.storageGb = storageGb;
    }

    public List<String> getAnsiblePlaybooks() {
        return ansiblePlaybooks;
    }

    public void setAnsiblePlaybooks(List<String> ansiblePlaybooks) {
        this.ansiblePlaybooks = ansiblePlaybooks;
    }

    public long getMemGb() {
        return memGb;
    }

    public void setMemGb(long memGb) {
        this.memGb = memGb;
    }

    public long getCores() {
        return cores;
    }

    public void setCores(long cores) {
        this.cores = cores;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
