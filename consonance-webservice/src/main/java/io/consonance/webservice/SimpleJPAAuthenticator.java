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

package io.consonance.webservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

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
                LOG.info("SimpleJPAAuthenticator called with " + credentials);
                final ConsonanceUser userByName = dao.findUserByHashedPassword(credentials);
                if (userByName != null){
                        return Optional.of(userByName);
                }
                return Optional.absent();
        }
}
