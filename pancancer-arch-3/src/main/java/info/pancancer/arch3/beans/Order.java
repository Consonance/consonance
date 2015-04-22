package info.pancancer.arch3.beans;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Order {

    Job job = null;
    Provision provision = null;
    String orderUUID = UUID.randomUUID().toString().toLowerCase();

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
                "  \"job\": "+job.toJSON()+"\n" +
                "  },\n" +
                "  \"provision\" : "+provision.toJSON()+"\n" +
                "}";
        return(json);
    }

    public Order fromJSON(String json) {

        return(this);

    }

}
