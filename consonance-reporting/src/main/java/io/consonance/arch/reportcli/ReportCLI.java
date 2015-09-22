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
package io.consonance.arch.reportcli;

import com.google.common.base.Joiner;
import io.consonance.arch.persistence.PostgreSQL;
import info.consonance.arch.Base;
import io.consonance.arch.reporting.ReportAPI;
import io.consonance.arch.reporting.ReportAPIFactory;
import info.consonance.arch.reporting.SlackRenderer;
import io.consonance.arch.utils.Utilities;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.consonance.arch.persistence.PostgreSQL;
import io.consonance.arch.reporting.ReportAPI;
import io.consonance.arch.reporting.ReportAPIFactory;
import io.consonance.arch.utils.Utilities;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

/**
 * This allows for ad-hoc reporting, should not be used at the same time as the ReportBot
 *
 * @author dyuen
 */
public class ReportCLI extends Base {

    public static final int SLEEP_IN_MILLISECONDS = 5000;

    public static void main(String[] argv) throws Exception {
        ReportCLI bot = new ReportCLI(argv);
        bot.doWork();
    }

    public ReportCLI(String[] argv) throws IOException {
        super();
        parseOptions(argv);
    }

    public void doWork() throws Exception {

        final HierarchicalINIConfiguration settings = Utilities.parseConfig(configFile);
        final PostgreSQL db = new PostgreSQL(settings);

        ReportAPI reportAPI = ReportAPIFactory.makeReportAPI(settings, db);
        // later, we'll replace his with a proper CLI renderer
        SlackRenderer renderer = new SlackRenderer(reportAPI);
        List<?> nonOptionArguments = options.nonOptionArguments();
        String joined = Joiner.on("").join(nonOptionArguments).toUpperCase(Locale.CANADA);
        SlackRenderer.FormattedMessage result = renderer.convertToResult(joined);
        if (result.message != null) {
            System.out.print(renderer.convertToResult(joined).message);
        }
        if (result.attachment != null) {
            System.out.print(renderer.convertToResult(joined).attachment.toString());
        }
    }
}
