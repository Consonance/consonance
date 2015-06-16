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
        if (message.startsWith("STATUS")) {
            StringBuilder builder = new StringBuilder();
            builder.append("*Historical VM counts*:\n");
            for (Entry<ProvisionState, Long> entry : reportAPI.getVMStateCounts().entrySet()) {
                builder.append(entry.getKey().toString()).append(": ").append(entry.getValue()).append("\n");
            }
            builder.append("*Historical Job counts*:\n");
            for (Entry<JobState, Integer> entry : reportAPI.getJobStateCounts().entrySet()) {
                builder.append(entry.getKey().toString()).append(": ").append(entry.getValue()).append("\n");
            }
            return new FormattedMessage(builder.toString(), null);
        } else if (message.startsWith("INFO")) {
            StringBuilder builder = new StringBuilder();
            for (Entry<String, String> entry : reportAPI.getEnvironmentMap().entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return new FormattedMessage(builder.toString(), null);
        } else if (message.startsWith("PROVISIONED")) {
            return new FormattedMessage("this would be a list of provisioned VMs", null);
        } else if (message.startsWith("JOBS")) {
            return new FormattedMessage("this would be a list of jobs", null);
        } else if (message.startsWith("GATHER")) {
            Map<String, Status> cache = reportAPI.getLastStatus();
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Status> entry : cache.entrySet()) {
                builder.append("*").append(entry.getKey()).append(" reports*:\n");
                String stdout = "`" + StringUtils.substringAfterLast(entry.getValue().getStdout(), "`\n");
                builder.append(stdout).append("\n");
            }
            SlackAttachment attach = new SlackAttachment("Messages gathered at " + new Date(), "Messages gathered", builder.toString(),
                    null);
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
