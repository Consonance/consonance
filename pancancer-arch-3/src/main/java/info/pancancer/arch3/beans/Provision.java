package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import net.minidev.json.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Provision {

    long cores;
    long memGb;
    long storageGb;
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
        uuid = (String) obj.get("uuid");
        JSONArray playbooks = (JSONArray) obj.get("bindle_profiles_to_run");
        ansiblePlaybooks.clear();
        for (Object key : playbooks) {
            ansiblePlaybooks.add((String)key);
        }
        return(this);

    }

}
