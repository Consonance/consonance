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
package io.consonance.webservice.jdbi;

import io.consonance.arch.beans.Job;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.Criteria;
import java.util.List;

/**
 *
 * @author dyuen
 */
public class JobDAO extends AbstractDAO<Job> {

    public JobDAO(SessionFactory factory) {
        super(factory);
    }

    public Job findById(int id) {
        return get(id);
    }

    public int create(Job job) {
        return persist(job).getJobId();
    }

    public List<Job> findAll() {
        return list(namedQuery("io.consonance.arch.beans.core.Job.findAll"));
    }

    public List<Job> findPaged(int first, int max) {
        Criteria criteria = criteria();
        criteria.setFirstResult(first);
        criteria.setMaxResults(max);
        return (criteria.list());
    }

    public List<Job> findPaged(int first, int max, String endUser) {
        return list(namedQuery("io.consonance.arch.beans.core.Job.findAllByUser").setString("endUser",endUser).setFirstResult(first).setMaxResults(max));
    }

    public Job findJobByUUID(String uuid){
        return uniqueResult(namedQuery("io.consonance.arch.beans.core.Job.findByJobUUID").setString("jobuuid",uuid));
    }

    public List<Job> findAll(String endUser) {
        return list(namedQuery("io.consonance.arch.beans.core.Job.findAllByUser").setString("endUser",endUser));
    }

    public Long findAllCount() {
        return (Long) namedQuery("io.consonance.arch.beans.core.Job.findAllCount").uniqueResult();
    }

    public Long findAllCount(String endUser) {
        return (Long) namedQuery("io.consonance.arch.beans.core.Job.findAllCountByUser").setString("endUser",endUser).uniqueResult();
    }



}
