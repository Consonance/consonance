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
package io.consonance.arch.reportbot;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import io.cloudbindle.youxia.util.Log;
import io.consonance.arch.Base;
import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.arch.reporting.ReportAPI;
import io.consonance.arch.reporting.ReportAPIFactory;
import io.consonance.arch.reporting.SlackRenderer;
import io.consonance.common.CommonTestUtilities;
import io.consonance.common.Constants;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import java.io.IOException;
import java.util.Locale;

/**
 * This responds to Slack events and translates them into reporting calls.
 *
 * @author dyuen
 */
public class SlackReportBot extends Base {

    public static final int SLEEP_IN_MILLISECONDS = 5000;

    public static void main(String[] argv) throws Exception {
        SlackReportBot bot = new SlackReportBot(argv);
        bot.doWork();
    }

    public SlackReportBot(String[] argv) throws IOException {
        super();
        parseOptions(argv);
    }

    public void doWork() throws Exception {

        final HierarchicalINIConfiguration settings = CommonTestUtilities.parseConfig(configFile);
        final PostgreSQL db = new PostgreSQL(settings);

        final SlackSession session = SlackSessionFactory.createWebSocketSlackSession(settings.getString(Constants.REPORT_TOKEN));
        session.addMessagePostedListener(new SlackMessagePostedListener() {
            @Override
            public void onEvent(SlackMessagePosted event, SlackSession session) {
                Log.debug(event.toString());
                String namespace = settings.getString(Constants.REPORT_NAMESPACE).toUpperCase(Locale.CANADA);
                if (event.getSender().getUserName().equalsIgnoreCase(namespace)) {
                    return;
                }
                // note that this ignores case
                String message = event.getMessageContent().toUpperCase(Locale.CANADA);

                boolean validMessage = false;
                // remove the '@' if necessary
                String dmIdentifer = "<@" + session.sessionPersona().getId() + ">";
                if (message.startsWith(dmIdentifer)) {
                    message = message.replaceFirst(dmIdentifer, "").trim();
                    // trim automatically inserted colon in some chat clients
                    if (message.startsWith(":")) {
                        message = message.replaceFirst(":", "").trim();
                    }
                    validMessage = true;
                }
                // remove plain user name if needed
                if (message.startsWith(namespace)) {
                    validMessage = true;
                    message = message.replaceFirst(namespace, "").trim();
                    Log.info("message was *" + message + "* ");
                    // remove extra colon that the Mac client adds
                    if (message.startsWith(":")) {
                        message = message.substring(1).trim();
                    }
                }

                if (validMessage) {
                    ReportAPI reportAPI = ReportAPIFactory.makeReportAPI(settings, db);
                    SlackRenderer renderer = new SlackRenderer(reportAPI);
                    SlackRenderer.FormattedMessage result = renderer.convertToResult(message);
                    if (result.attachment != null) {
                        result.attachment.addMarkdownIn("text");
                    }
                    session.sendMessage(session.findChannelById(event.getChannel().getId()), result.message, result.attachment);
                }
            }
        });

        int retries = 0;
        boolean retry = false;
        do {
            try {
                session.connect();
                do {
                    Thread.sleep(SLEEP_IN_MILLISECONDS);
                } while (options.has(this.endlessSpec));
            } catch (Exception e) {
                Log.error("SlackBot connection issue", e);
                long waitTime = Math.min(getWaitTimeExp(retries), TEN_MINUTES_IN_MILLISECONDS);
                System.out.print(waitTime + "\n");
                // Wait for the result.
                Thread.sleep(waitTime);
            }
        } while (options.has(this.endlessSpec));
    }

    /*
     * Returns the next wait interval, in milliseconds, using an exponential backoff algorithm. Taken from AWS.
     */
    public static long getWaitTimeExp(int retryCount) {
        final long convertToMilliseconds = 100L;
        return ((long) Math.pow(2, retryCount) * convertToMilliseconds);
    }
}
