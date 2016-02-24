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

import io.consonance.arch.utils.CommonServerTestUtilities;
import org.json.simple.JSONObject;

import java.util.UUID;

/**
 * This is an Order which represents a request which gets processed by the Co-ordinator
 *
 * @author boconnor
 * @author dyuen
 */
public class Order {

    CommonServerTestUtilities u = new CommonServerTestUtilities();
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
