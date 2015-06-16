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
package info.pancancer.arch3.reportbot;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import info.pancancer.arch3.Base;
import info.pancancer.arch3.persistence.PostgreSQL;
import info.pancancer.arch3.reporting.ReportAPI;
import info.pancancer.arch3.reporting.ReportAPIFactory;
import info.pancancer.arch3.reporting.SlackRenderer;
import info.pancancer.arch3.utils.Constants;
import info.pancancer.arch3.utils.Utilities;
import java.io.IOException;
import java.util.Locale;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

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

        final HierarchicalINIConfiguration settings = Utilities.parseConfig(configFile);
        final PostgreSQL db = new PostgreSQL(settings);

        final SlackSession session = SlackSessionFactory.createWebSocketSlackSession(settings.getString(Constants.REPORT_TOKEN));
        session.addMessagePostedListener(new SlackMessagePostedListener() {
            @Override
            public void onEvent(SlackMessagePosted event, SlackSession session) {
                System.out.println(event.toString());
                String namespace = settings.getString(Constants.REPORT_NAMESPACE).toUpperCase(Locale.CANADA);
                if (event.getSender().getUserName().equalsIgnoreCase(namespace)) {
                    return;
                }
                // note that this ignores case
                String message = event.getMessageContent().toUpperCase(Locale.CANADA);

                boolean validMessage = false;
                // remove the '@' if necessary
                String dmIdentifer = "<@" + session.sessionPersona().getId() + ">:";
                if (message.startsWith(dmIdentifer)) {
                    message = message.replaceFirst(dmIdentifer, "");
                    validMessage = true;
                }
                // remove plain user name if needed
                if (message.startsWith(namespace)) {
                    validMessage = true;
                    message = message.replaceFirst(namespace, "").trim();
                }
                if (validMessage) {
                    ReportAPI reportAPI = ReportAPIFactory.makeReportAPI(settings, db);
                    SlackRenderer renderer = new SlackRenderer(reportAPI);
                    SlackRenderer.FormattedMessage result = renderer.convertToResult(message);
                    session.sendMessage(session.findChannelById(event.getChannel().getId()), result.message, result.attachment);
                }
            }
        });
        session.connect();

        do {
            Thread.sleep(SLEEP_IN_MILLISECONDS);
        } while (options.has(this.endlessSpec));
    }
}
