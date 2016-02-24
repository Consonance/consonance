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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.sql.Timestamp;

/**
 * @author dyuen
 */
@MappedSuperclass public abstract class BaseBean {

        @Column(name = "create_timestamp", columnDefinition = "timestamp")
        @CreationTimestamp
        private Timestamp createTimestamp;
        @Column(name = "update_timestamp", columnDefinition = "timestamp")
        @UpdateTimestamp
        private Timestamp updateTimestamp;

        public String toJSON() {
                ObjectMapper mapper = new ObjectMapper();
                mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                try {
                        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return null;
                }
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
