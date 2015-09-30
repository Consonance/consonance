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
