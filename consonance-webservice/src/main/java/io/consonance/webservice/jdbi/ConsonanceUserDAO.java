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
package io.consonance.webservice.jdbi;

import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;

/**
 *
 * @author dyuen
 */
public class ConsonanceUserDAO extends AbstractDAO<ConsonanceUser> {
    public ConsonanceUserDAO(SessionFactory factory) {
        super(factory);
    }

    public ConsonanceUser findById(int id) {
        return get(id);
    }

    public int create(ConsonanceUser user) {
        return persist(user).getUserID();
    }

    public List<ConsonanceUser> findAll() {
        return list(namedQuery("io.consonance.webservice.core.ConsonanceUser.findAll"));
    }

    public ConsonanceUser findUserByName(String name){
        return uniqueResult(namedQuery("io.consonance.webservice.core.ConsonanceUser.findByName").setString("name",name));
    }

    public ConsonanceUser findUserByHashedPassword(String hashedPassword){
        return uniqueResult(namedQuery("io.consonance.webservice.core.ConsonanceUser.findByPassword").setString("hashedPassword",hashedPassword));
    }
}
