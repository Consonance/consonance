package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by boconnor on 2015-04-22.
 */
public class Status {

    private Utilities u = new Utilities();
    private String type = null;
    private String state = null;
    private String uuid = null;
    private String message = null;

    public Status(String uuid, String state, String type, String message) {
        this.uuid = uuid;
        this.state = state;
        this.type = type;
        this.message = message;
    }

    public Status() {
        super();
    }

    public String toJSON () {

        StringBuffer j = new StringBuffer();
        j.append("{" +
                "\"uuid\": \""+uuid+"\",\n" +
                "\"type\": \""+type+"\",\n" +
                "\"state\": \""+state+"\",\n" +
                "\"message\": \""+message+"\"\n" +
                "}\n");
        return(j.toString());
    }

    public Status fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        uuid = (String) obj.get("uuid");
        state = (String) obj.get("state");
        message = (String) obj.get("message");
        type = (String) obj.get("type");

        return(this);

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
