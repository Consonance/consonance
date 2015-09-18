package info.consonance.arch.beans;

import info.consonance.arch.utils.Utilities;
import java.util.UUID;
import org.json.simple.JSONObject;

/**
 * This is an Order which represents a request which gets processed by the Co-ordinator
 *
 * @author boconnor
 * @author dyuen
 */
public class Order {

    Utilities u = new Utilities();
    private Job job = null;
    private Provision provision = null;
    String orderUUID = UUID.randomUUID().toString().toLowerCase();

    public Order() {
        super();
    }

    public String toJSON() {
        String json = "{ \n" + "  \"message_type\": \"order\",\n" + "  \"order_uuid\": \"" + orderUUID + "\",\n" + "  \"job\": "
                + job.toJSON() + ",\n" + "  \"provision\": " + provision.toJSON() + "\n" + "}";
        return json;
    }

    public Order fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        job = new Job().fromJSON(obj.get("job").toString());
        provision = new Provision().fromJSON(obj.get("provision").toString());
        orderUUID = (String) obj.get("order_uuid");

        return this;

    }

    public Provision getProvision() {
        return provision;
    }

    public Job getJob() {
        return job;
    }

    /**
     * @param job
     *            the job to set
     */
    public void setJob(Job job) {
        this.job = job;
    }

    /**
     * @param provision
     *            the provision to set
     */
    public void setProvision(Provision provision) {
        this.provision = provision;
    }

}
