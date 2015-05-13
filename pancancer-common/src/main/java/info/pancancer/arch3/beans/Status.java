package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import org.json.simple.JSONObject;

/**
 * This represents a Status which is a message sent to the Results/Status queue. Created by boconnor on 2015-04-22.
 */
public class Status {

    private final Utilities u = new Utilities();
    private String type = null;
    private StatusState state = null;
    private String vmUuid = null;
    private String jobUuid = null;
    private String message = null;
    private String stderr = null;
    private String stdout = null;
    private String ipAddress = null;

    public Status(String vmUuid, String jobUuid, StatusState state, String type, String message,
            String ipAddress) {
        this.vmUuid = vmUuid;
        this.jobUuid = jobUuid;
        this.state = state;
        this.type = type;
        this.message = message;
        this.ipAddress = ipAddress;
    }

    public Status() {
        super();
    }

    public String toJSON() {

        StringBuilder j = new StringBuilder();
        j.append("{" + "\"vmUuid\": \"").append(vmUuid).append("\",\n" + "\"jobUuid\": \"").append(jobUuid)
                .append("\",\n" + "\"type\": \"").append(type).append("\",\n" + "\"state\": \"").append(state)
                .append("\",\n" + "\"stderr\": \"").append(stderr).append("\",\n" + "\"stdout\": \"").append(stdout)
                .append("\",\n" + "\"ipAddress\": \"").append(ipAddress).append("\",\n" + "\"message\": \"").append(message)
                .append("\"\n" + "}\n");
        return j.toString();
    }

    public Status fromJSON(String json) {

        JSONObject obj = u.parseJob(json);
        jobUuid = (String) obj.get("jobUuid");
        vmUuid = (String) obj.get("vmUuid");
        state = StatusState.valueOf((String) obj.get("state"));
        message = (String) obj.get("message");
        type = (String) obj.get("type");
        stderr = (String) obj.get("stderr");
        stdout = (String) obj.get("stdout");
        ipAddress = (String) obj.get("ipAddress");

        return this;

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public StatusState getState() {
        return state;
    }

    public void setState(StatusState state) {
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

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress
     *            the ipAddress to set
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
