/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package info.pancancer.arch3.beans;

import info.pancancer.arch3.utils.Utilities;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * This represents a message sent to the Container/VM queue. Created by boconnor on 2015-04-22.
 */
public class Provision {

    private long cores;
    private long memGb;
    private long storageGb;
    private ProvisionState state = ProvisionState.START;
    private List<String> ansiblePlaybooks;
    private String ipAddress = "";
    private String jobUUID = "";
    /**
     * This is the provision_uuid
     */
    private String provisionUUID = "";
    private Timestamp createTimestamp;
    private Timestamp updateTimestamp;

    public Provision(int cores, int memGb, int storageGb, List<String> ansiblePlaybooks) {
        this.cores = cores;
        this.memGb = memGb;
        this.storageGb = storageGb;
        this.ansiblePlaybooks = ansiblePlaybooks;
    }

    public Provision() {
        super();
    }

    public String toJSON() {

        StringBuilder j = new StringBuilder();

        j.append("{" + "   \"message_type\": \"provision\",\n" + "\"provision_uuid\": \"").append(provisionUUID)
                .append("\",\n" + "   \"cores\": ").append(cores).append(",\n" + "    \"mem_gb\": ").append(memGb)
                .append(",\n" + "    \"storage_gb\": ").append(storageGb).append(",\n" + "    \"job_uuid\": \"").append(jobUUID)
                .append("\",\n" + "    \"ip_address\": \"").append(ipAddress).append("\",\n" + "    \"bindle_profiles_to_run\": [");

        boolean first = true;
        for (String playbook : ansiblePlaybooks) {
            if (first) {
                first = false;
            } else {
                j.append(",\n");
            }
            j.append("\"").append(playbook).append("\"");
        }
        j.append("\n]\n}\n");
        return (j.toString());
    }

    public Provision fromJSON(String json) {
        Utilities u = new Utilities();
        JSONObject obj = u.parseJob(json);
        cores = (Long) obj.get("cores");
        memGb = (Long) obj.get("mem_gb");
        storageGb = (Long) obj.get("storage_gb");
        this.setJobUUID((String) obj.get("job_uuid"));
        this.setIpAddress((String) obj.get("ip_address"));
        provisionUUID = (String) obj.get("provision_uuid");
        JSONArray playbooks = (JSONArray) obj.get("bindle_profiles_to_run");
        ansiblePlaybooks = new ArrayList<>();
        for (Object key : playbooks) {
            ansiblePlaybooks.add((String) key);
        }
        return this;

    }

    public String getProvisionUUID() {
        return provisionUUID;
    }

    public void setProvisionUUID(String provisionUUID) {
        this.provisionUUID = provisionUUID;
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

    public ProvisionState getState() {
        return state;
    }

    public void setState(ProvisionState state) {
        this.state = state;
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

    /**
     * @return the jobUUID
     */
    public String getJobUUID() {
        return jobUUID;
    }

    /**
     * @param jobUUID
     *            the jobUUID to set
     */
    public void setJobUUID(String jobUUID) {
        this.jobUUID = jobUUID;
    }

    /**
     * @return the createTimestamp
     */
    public Timestamp getCreateTimestamp() {
        return createTimestamp;
    }

    /**
     * @param createTimestamp the createTimestamp to set
     */
    public void setCreateTimestamp(Timestamp createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    /**
     * @return the updateTimestamp
     */
    public Timestamp getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * @param updateTimestamp the updateTimestamp to set
     */
    public void setUpdateTimestamp(Timestamp updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }
}
