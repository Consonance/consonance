package io.consonance.webservice.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.consonance.arch.beans.BaseBean;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.security.Principal;
import java.util.Objects;

/**
 * A quick and dirty way to store consonance users for basic auth.
 * @author dyuen
 */
@Entity
@Table(name= "consonance_user")
@NamedQueries({
        @NamedQuery(
                name = "io.consonance.webservice.core.ConsonanceUser.findAll",
                query = "SELECT u FROM ConsonanceUser u"
        ),
        @NamedQuery(
                name = "io.consonance.webservice.core.ConsonanceUser.findByName",
                query = "SELECT u FROM ConsonanceUser u WHERE name = :name"
        ),
        @NamedQuery(
                name = "io.consonance.webservice.core.ConsonanceUser.findByPassword",
                query = "SELECT u FROM ConsonanceUser u WHERE hashedPassword = :hashedPassword"
        )
})
public class ConsonanceUser extends BaseBean implements Principal  {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name="user_id")
        @ApiModelProperty(name="user_id", value = "db id")
        private int userID;

        @ApiModelProperty(value = "username")
        @Column(unique = true)
        private String name;

        @ApiModelProperty(value = "set this if creating an admin")
        private boolean admin;

        @JsonIgnore
        @Column(name="hashed_password")
        @ApiModelProperty(value = "the hashed password, should not be transfered over webservice", hidden=true)
        private String hashedPassword;

        public int getUserID() {
                return userID;
        }

        public void setUserID(int userID) {
                this.userID = userID;
        }

        @Override public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getHashedPassword() {
                return hashedPassword;
        }

        public void setHashedPassword(String hashedPassword) {
                this.hashedPassword = hashedPassword;
        }

        public boolean isAdmin() {
                return admin;
        }

        public void setAdmin(boolean admin) {
                this.admin = admin;
        }

        public int hashCode(){
                return Objects.hash(name, hashedPassword);
        }


        public boolean equals(Object other){
                if (other instanceof ConsonanceUser) {
                        ConsonanceUser otherUser = (ConsonanceUser)other;
                        return Objects.equals(this.getName(), otherUser.getName()) && Objects
                                .equals(this.getHashedPassword(), otherUser.getHashedPassword());
                }
                return false;
        }
}
