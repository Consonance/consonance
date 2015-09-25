package io.consonance.webservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;

import io.consonance.webservice.core.ConsonanceUser;
import io.consonance.webservice.jdbi.ConsonanceUserDAO;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

/**
 * @author dyuen
 */
public class SimpleJPAAuthenticator implements Authenticator<String, ConsonanceUser> {

        private final ConsonanceUserDAO dao;
        private static final Logger LOG = LoggerFactory.getLogger(SimpleJPAAuthenticator.class);

        public SimpleJPAAuthenticator(ConsonanceUserDAO dao){
                this.dao = dao;
        }

        @Override
        public Optional<ConsonanceUser> authenticate(String credentials) throws AuthenticationException {
                LOG.error("SimpleJPAAuthenticator called with " + credentials);
                final String hashedPassword = Hashing
                        .sha256()
                        .hashString(credentials, Charsets.UTF_8)
                        .toString();
                final ConsonanceUser userByName = dao.findUserByHashedPassword(hashedPassword);
                if (userByName != null){
                        return Optional.of(userByName);
                }
                return Optional.absent();
        }
}
