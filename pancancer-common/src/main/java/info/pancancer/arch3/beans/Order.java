package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Order {

    Utilities u = new Utilities();
    Job job = null;
    Provision provision = null;
    String orderUUID = UUID.randomUUID().toString().toLowerCase();

    public Order () {
        super();
    }

    public Order (String workflow, String workflowVersion, String jobHash, Map<String, String> ini,
                  int cores, int memGb, int storageGb, List<String> ansiblePlaybooks) {

        this.job = new Job(workflow, workflowVersion, jobHash, ini);
        this.provision = new Provision(cores, memGb, storageGb, ansiblePlaybooks);
    }

    public String toJSON() {
        String json =
        "{ \n" +
        "  \"message_type\": \"order\",\n" +
        "  \"order_uuid\": \""+orderUUID+"\",\n" +
        "  \"job\": "+job.toJSON()+",\n" +
        "  \"provision\": "+provision.toJSON()+"\n" +
        "}";
        return(json);
    }

    public Order fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        job = new Job().fromJSON(obj.get("job").toString());
        provision = new Provision().fromJSON(obj.get("provision").toString());
        orderUUID = (String) obj.get("order_uuid");

        return(this);

    }

    public Provision getProvision() {
        return provision;
    }

    public Job getJob() {
        return job;
    }

}
