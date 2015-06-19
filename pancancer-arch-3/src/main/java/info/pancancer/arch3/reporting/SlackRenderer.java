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

import com.ullink.slack.simpleslackapi.SlackAttachment;
import info.pancancer.arch3.beans.JobState;
import info.pancancer.arch3.beans.ProvisionState;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.reporting.ReportAPI.Commands;
import io.cloudbindle.youxia.listing.AbstractInstanceListing;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
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
        if (message.startsWith(Commands.STATUS.name())) {
            StringBuilder builder = new StringBuilder();

            builder.append("*Active VM counts via youxia*:\n");
            Map<String, AbstractInstanceListing.InstanceDescriptor> awsInstances = reportAPI.getYouxiaInstances("AWS");
            Map<String, AbstractInstanceListing.InstanceDescriptor> osInstances = reportAPI.getYouxiaInstances("OPENSTACK");
            builder.append(awsInstances.size()).append(" instances managed on AWS \n");
            builder.append(osInstances.size()).append(" instances managed on OpenStack \n");

            builder.append("*Historical VM counts*:\n");
            for (Entry<ProvisionState, Long> entry : reportAPI.getVMStateCounts().entrySet()) {
                builder.append(entry.getKey().toString()).append(": ").append(entry.getValue()).append("\n");
            }
            builder.append("*Historical Job counts*:\n");
            for (Entry<JobState, Integer> entry : reportAPI.getJobStateCounts().entrySet()) {
                builder.append(entry.getKey().toString()).append(": ").append(entry.getValue()).append("\n");
            }

            return new FormattedMessage(builder.toString(), null);
        } else if (message.startsWith(Commands.YOUXIA.name())) {
            Map<String, AbstractInstanceListing.InstanceDescriptor> awsInstances = reportAPI.getYouxiaInstances("AWS");
            Map<String, AbstractInstanceListing.InstanceDescriptor> osInstances = reportAPI.getYouxiaInstances("OPENSTACK");
            StringBuilder builder = new StringBuilder();
            if (awsInstances.size() > 0) {
                builder.append("*").append("Instances on AWS").append("*:\n");
                for (Entry<String, AbstractInstanceListing.InstanceDescriptor> entry : awsInstances.entrySet()) {
                    builder.append("*").append(entry.getKey()).append("*:\n");
                    builder.append("ip address").append(" ").append(entry.getValue().getIpAddress()).append(":\n");
                    builder.append("private ip address").append(" ").append(entry.getValue().getPrivateIpAddress()).append(":\n");
                    builder.append("spot instance").append(" ").append(entry.getValue().isSpotInstance()).append(":\n");
                }
            }
            // this sucks, make this better
            if (osInstances.size() > 0) {
                builder.append("*").append("Instances on OpenStack").append("*:\n");
                for (Entry<String, AbstractInstanceListing.InstanceDescriptor> entry : osInstances.entrySet()) {
                    builder.append("*").append(entry.getKey()).append("*:\n");
                    builder.append("ip address").append(" ").append(entry.getValue().getIpAddress()).append(":\n");
                    builder.append("private ip address").append(" ").append(entry.getValue().getPrivateIpAddress()).append(":\n");
                }
            }
            SlackAttachment attach = new SlackAttachment("Live cloud instance info described on " + new Date(), "Live instances",
                    builder.toString(), null);
            return new FormattedMessage(null, attach);

        } else if (message.startsWith(Commands.INFO.name())) {
            StringBuilder builder = new StringBuilder();
            for (Entry<String, String> entry : reportAPI.getEnvironmentMap().entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return new FormattedMessage(builder.toString(), null);
        } else if (message.startsWith(Commands.PROVISIONED.name())) {
            Map<String, Map<String, String>> jobInfo = reportAPI.getVMInfo();
            StringBuilder builder = new StringBuilder();
            for (Entry<String, Map<String, String>> entry : jobInfo.entrySet()) {
                builder.append("*").append(entry.getKey()).append("*\n");
                for (Entry<String, String> innerEntry : entry.getValue().entrySet()) {
                    builder.append(innerEntry.getKey()).append(" ").append(innerEntry.getValue()).append("\n");
                }
            }
            SlackAttachment attach = new SlackAttachment("VM info from DB at " + new Date(), "VMs from DB", builder.toString(), null);
            return new FormattedMessage(null, attach);

        } else if (message.startsWith(Commands.JOBS.name())) {
            Map<String, Map<String, String>> jobInfo = reportAPI.getJobInfo();
            StringBuilder builder = new StringBuilder();
            for (Entry<String, Map<String, String>> entry : jobInfo.entrySet()) {
                builder.append("*").append(entry.getKey()).append("*\n");
                for (Entry<String, String> innerEntry : entry.getValue().entrySet()) {
                    builder.append(innerEntry.getKey()).append(" ").append(innerEntry.getValue()).append("\n");
                }
            }
            SlackAttachment attach = new SlackAttachment("Job info from DB at " + new Date(), "Jobs from DB", builder.toString(), null);
            return new FormattedMessage(null, attach);
        } else if (message.startsWith(Commands.GATHER.name())) {
            Map<String, Status> cache = reportAPI.getLastStatus();
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Status> entry : cache.entrySet()) {
                builder.append("*").append(entry.getKey()).append(" reports*:\n");
                String stdout = "`" + StringUtils.substringAfterLast(entry.getValue().getStdout(), "\n") + "`";
                builder.append(stdout).append("\n");
            }
            SlackAttachment attach = new SlackAttachment("Messages gathered from queues at " + new Date(), "Messages gathered",
                    builder.toString(), null);
            return new FormattedMessage(null, attach);
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Available commands are:\n");
            for (Map.Entry<String, String> entry : reportAPI.getCommands().entrySet()) {
                builder.append("`").append(entry.getKey()).append("` ");
                builder.append(entry.getValue()).append("\n");
            }
            return new FormattedMessage(builder.toString(), null);
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
