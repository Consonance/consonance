/*
 * Copyright (C) 2015 CancerCollaboratory
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
package info.pancancer.arch3.reporting;

import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.beans.Status;
import java.util.Map;

/**
 * This defines methods that retrieve data that can subsequently be formatted for particular destinations.
 *
 * @author dyuen
 */
public interface ReportAPI {

    Map<ProvisionState, Long> getVMStateCounts();

    Map<JobState, Integer> getJobStateCounts();

    Map<String, Status> getLastStatus();

    Map<String, String> getEnvironmentMap();

    Map<String, String> getCommands();

}
