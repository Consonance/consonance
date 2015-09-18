/*
 * Copyright (C) 2015 Consonance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.consonance.webservice.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import info.consonance.arch.beans.StatusState;
import io.swagger.annotations.ApiModel;

import java.sql.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author dyuen
 */
@ApiModel(value = "A particular workflow run that a user has submitted")
@Entity
@Table(name = "workflowrun")
public class WorkflowRun {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String collabJSON;
    @Column(nullable = false)
    private String owner;
    @Column(nullable = false)
    private StatusState status;
    @Column(name="create_timestamp")
    @Temporal(TemporalType.DATE)
    private Date createTimestamp;
    @Column(name="update_timestamp")
    @Temporal(TemporalType.DATE)
    private Date updateTimestamp;

    public WorkflowRun() {
    }

    public WorkflowRun(long id, String owner) {
        this.id = id;
        this.owner = owner;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    /**
     * @return the collabJSON
     */
    @JsonProperty
    public String getCollabJSON() {
        return collabJSON;
    }

    /**
     * @param collabJSON
     *            the collabJSON to set
     */
    public void setCollabJSON(String collabJSON) {
        this.collabJSON = collabJSON;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowRun)) {
            return false;
        }

        final WorkflowRun that = (WorkflowRun) o;

        return Objects.equals(this.id, that.id) && Objects.equals(this.collabJSON, that.collabJSON)
                && Objects.equals(this.owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, collabJSON, owner);
    }

    /**
     * @return the owner
     */
    @JsonProperty
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the status
     */
    @JsonProperty
    public StatusState getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(StatusState status) {
        this.status = status;
    }

}
