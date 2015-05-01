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
    private String vmUuid = null;
    private String jobUuid = null;
    private String message = null;
    private String stderr = null;
    private String stdout = null;

    public Status(String vmUuid, String jobUuid, String state, String type, String stderr, String stdout, String message) {
        this.vmUuid = vmUuid;
        this.jobUuid = jobUuid;
        this.state = state;
        this.type = type;
        this.stderr = stderr;
        this.stdout = stdout;
        this.message = message;
    }

    public Status() {
        super();
    }

    public String toJSON () {

        StringBuffer j = new StringBuffer();
        j.append("{" +
                "\"vmUuid\": \""+vmUuid+"\",\n" +
                "\"jobUuid\": \""+jobUuid+"\",\n" +
                "\"type\": \""+type+"\",\n" +
                "\"state\": \""+state+"\",\n" +
                "\"stderr\": \""+stderr+"\",\n" +
                "\"stdout\": \""+stdout+"\",\n" +
                "\"message\": \""+message+"\"\n" +
                "}\n");
        return(j.toString());
    }

    public Status fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        jobUuid = (String) obj.get("jobUuid");
        vmUuid = (String) obj.get("vmUuid");
        state = (String) obj.get("state");
        message = (String) obj.get("message");
        type = (String) obj.get("type");
        stderr = (String) obj.get("stderr");
        stdout = (String) obj.get("stdout");

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }
}
