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
package io.consonance.arch.reporting;

import com.ullink.slack.simpleslackapi.SlackAttachment;
import io.consonance.arch.CloudTypes;
import io.consonance.arch.beans.ProvisionState;
import io.consonance.arch.beans.Status;
import io.consonance.arch.beans.JobState;
import io.cloudbindle.youxia.listing.AbstractInstanceListing;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import io.consonance.arch.CloudTypes;
import io.consonance.arch.beans.ProvisionState;
import org.apache.commons.lang3.StringUtils;

/**
 * THe SlackRenderer takes generic data from the report API and formats it for output to Slack.
 *
 * @author dyuen
 */
public class SlackRenderer {
    private final ReportAPI reportAPI;

    public SlackRenderer(ReportAPI reportAPI) {
        this.reportAPI = reportAPI;
    }

    public FormattedMessage convertToResult(String message) {
        String[] words = message.split(" ");

        String firstWord = words[0];
        ReportAPI.Commands firstCommand;
        try {
            firstCommand = ReportAPI.Commands.valueOf(firstWord);
        } catch (IllegalArgumentException ex) {
            StringBuilder builder = new StringBuilder();
            builder.append("Available commands are:\n");
            for (Map.Entry<String, String> entry : reportAPI.getCommands().entrySet()) {
                builder.append("`").append(entry.getKey()).append("` ");
                builder.append(entry.getValue()).append("\n");
            }
            return new FormattedMessage(builder.toString(), null);
        }

        Map<String, AbstractInstanceListing.InstanceDescriptor> awsInstances;
        Map<String, AbstractInstanceListing.InstanceDescriptor> osInstances;
        Map<String, AbstractInstanceListing.InstanceDescriptor> azureInstances;
        StringBuilder builder = new StringBuilder();
        SlackAttachment attach;
        Map<String, Map<String, String>> jobInfo;

        switch (firstCommand) {

        case STATUS:

            builder.append("*Active VM counts via youxia*:\n");
            awsInstances = reportAPI.getYouxiaInstances(CloudTypes.AWS);
            osInstances = reportAPI.getYouxiaInstances(CloudTypes.OPENSTACK);
            azureInstances = reportAPI.getYouxiaInstances(CloudTypes.AZURE);
            builder.append(awsInstances.size()).append(" instances managed on AWS \n");
            builder.append(osInstances.size()).append(" instances managed on OpenStack \n");
            builder.append(azureInstances.size()).append(" instances managed on Azure \n");

            builder.append("*Historical VM counts*:\n");
            for (Entry<ProvisionState, Long> entry : reportAPI.getVMStateCounts().entrySet()) {
                builder.append(entry.getKey().toString()).append(": ").append(entry.getValue()).append("\n");
            }
            builder.append("*Historical Job counts*:\n");
            for (Entry<JobState, Integer> entry : reportAPI.getJobStateCounts().entrySet()) {
                builder.append(entry.getKey().toString()).append(": ").append(entry.getValue()).append("\n");
            }

            builder.append("\n");
            // determine the intersection of live vms with failed jobs on them
            Map<String, Map<String, String>> vmInfo = reportAPI.getVMInfo(ProvisionState.FAILED);
            if (vmInfo.isEmpty()) {
                builder.append("There are no failed jobs on VMs that require attention\n");
            } else {
                builder.append("*There are failed jobs on VMs that require attention:*\n");
                renderMapOfMaps(vmInfo, builder);
            }

            return new FormattedMessage(builder.toString(), null);

        case YOUXIA:
            awsInstances = reportAPI.getYouxiaInstances(CloudTypes.AWS);
            osInstances = reportAPI.getYouxiaInstances(CloudTypes.OPENSTACK);
            azureInstances = reportAPI.getYouxiaInstances(CloudTypes.AZURE);

            if (awsInstances.size() > 0) {
                renderInstances("AWS", builder, awsInstances);
            }
            if (awsInstances.size() > 0) {
                renderInstances("OpenStack", builder, osInstances);
            }
            if (awsInstances.size() > 0) {
                renderInstances("Azure", builder, azureInstances);
            }

            attach = new SlackAttachment("Live cloud instance info described on " + new Date(), "Live instances", builder.toString(), null);
            return new FormattedMessage(null, attach);

        case INFO:
            for (Entry<String, String> entry : reportAPI.getEnvironmentMap().entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return new FormattedMessage(builder.toString(), null);

        case PROVISIONED:
            jobInfo = reportAPI.getVMInfo();
            renderMapOfMaps(jobInfo, builder);
            attach = new SlackAttachment("VM info from DB at " + new Date(), "VMs from DB", builder.toString(), null);
            return new FormattedMessage(null, attach);

        case JOBS:
            jobInfo = reportAPI.getJobInfo();
            renderMapOfMaps(jobInfo, builder);
            attach = new SlackAttachment("Job info from DB at " + new Date(), "Jobs from DB", builder.toString(), null);
            return new FormattedMessage(null, attach);

        case GATHER:
            Map<String, Status> cache = reportAPI.getLastStatus();
            for (Map.Entry<String, Status> entry : cache.entrySet()) {
                builder.append("*").append(entry.getKey()).append(" reports*:\n");
                String stdout = "`" + StringUtils.substringAfterLast(entry.getValue().getStdout(), "\n") + "`";
                builder.append(stdout).append("\n");
            }
            attach = new SlackAttachment("Messages gathered from queues at " + new Date(), "Messages gathered", builder.toString(), null);
            return new FormattedMessage(null, attach);

        default:
            /** do nothing, not a valid command */
        }
        throw new RuntimeException("should not get here in the SlackRenderer");
    }

    private void renderInstances(String description, StringBuilder builder,
            Map<String, AbstractInstanceListing.InstanceDescriptor> instances) {
        builder.append("*").append("Instances on ").append(description).append("*:\n");
        for (Entry<String, AbstractInstanceListing.InstanceDescriptor> entry : instances.entrySet()) {
            builder.append("*").append(entry.getKey()).append("*:\n");
            builder.append("ip address").append(" ").append(entry.getValue().getIpAddress()).append(":\n");
            builder.append("private ip address").append(" ").append(entry.getValue().getPrivateIpAddress()).append(":\n");
            builder.append("spot instance").append(" ").append(entry.getValue().isSpotInstance()).append(":\n");
        }
    }

    private void renderMapOfMaps(Map<String, Map<String, String>> jobInfo, StringBuilder builder) {
        for (Entry<String, Map<String, String>> entry : jobInfo.entrySet()) {
            builder.append("*").append(entry.getKey()).append("*\n");
            for (Entry<String, String> innerEntry : entry.getValue().entrySet()) {
                builder.append(innerEntry.getKey()).append(" ").append(innerEntry.getValue()).append("\n");
            }
        }
    }

    public static class FormattedMessage {
        public final String message;
        public final SlackAttachment attachment;

        public FormattedMessage(String message, SlackAttachment attachment) {
            this.message = message;
            this.attachment = attachment;
        }
    }
}
