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
import io.cloudbindle.youxia.listing.AbstractInstanceListing;
import java.util.Locale;
import java.util.Map;

/**
 * This defines methods that retrieve data that can subsequently be formatted for particular destinations.
 *
 * @author dyuen
 */
public interface ReportAPI {

    public enum Commands {
        STATUS("retrieves configuration and version information on arch3"), INFO("retrieves high-level information on bot config"), PROVISIONED(
                "retrieves detailed information on provisioned instances"), JOBS("retrieves detailed information on jobs"), GATHER(
                "gathers the last message sent by each worker and displays the last line of it"), YOUXIA(
                "ask youxia for all information on instances known to the cloud APIs that are configured");
        private final String description;

        Commands(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.CANADA);
        }
    }

    /**
     * Get a listing of all instances known to youxia.
     *
     * @return
     */
    Map<String, AbstractInstanceListing.InstanceDescriptor> getYouxiaInstances();

    /**
     * Get a listing of all instances known to youxia for a particular cloud.
     *
     * @param cloudType
     * @return
     */
    Map<String, AbstractInstanceListing.InstanceDescriptor> getYouxiaInstances(String cloudType);

    /**
     * Get information for all jobs in the system, separated by unique identifier.
     *
     * @return
     */
    Map<String, Map<String, String>> getJobInfo();

    /**
     * Get information only on VMs in a specific set of states.
     *
     * @param states
     * @return
     */
    Map<String, Map<String, String>> getVMInfo(ProvisionState... states);

    /**
     * Get information for all provisions in the system, separated by unique identifier.
     *
     * This is hooked into youxia to only report on active instances
     *
     * @return
     */
    Map<String, Map<String, String>> getVMInfo();

    /**
     * Get counts of VMs in the system sorted by state.
     *
     * @return
     */
    Map<ProvisionState, Long> getVMStateCounts();

    /**
     * Get list of jobs by status.
     *
     * @return
     */
    Map<JobState, Integer> getJobStateCounts();

    /**
     * Get the last Status message sent by all workers in the system.
     *
     * @return
     */
    Map<String, Status> getLastStatus();

    /**
     * Get a map with all variables that are relevant in the system.
     *
     * @return
     */
    Map<String, String> getEnvironmentMap();

    /**
     * Get a list of all possible commands to respond to.
     *
     * @return
     */
    Map<String, String> getCommands();

}
