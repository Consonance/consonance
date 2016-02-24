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

package io.consonance.arch.beans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This represents a Status which is a message sent to the Results/Status queue. Created by boconnor on 2015-04-22.
 */
public class Status {

    private String type = null;
    private StatusState state = null;
    private String vmUuid = null;
    private String jobUuid = null;
    private String message = null;
    private String stderr = null;
    private String stdout = null;
    private String ipAddress = null;

    public Status(String vmUuid, String jobUuid, StatusState state, String type, String message, String ipAddress) {
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public Status fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Status.class);
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
