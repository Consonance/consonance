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
package io.consonance.arch.worker;

/**
 *
 * @author dyuen
 */
class WorkflowResult {
    private String workflowStdout = "no stdout";
    private String workflowStdErr = "no stderr";
    private int exitCode = Integer.MIN_VALUE;

    /**
     * @return the workflowStdout
     */
    String getWorkflowStdout() {
        return workflowStdout;
    }

    /**
     * @param workflowStdout
     *            the workflowStdout to set
     */
    void setWorkflowStdout(String workflowStdout) {
        this.workflowStdout = workflowStdout;
    }

    /**
     * @return the exitCode
     */
    int getExitCode() {
        return exitCode;
    }

    /**
     * @param exitCode
     *            the exitCode to set
     */
    void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * @return the workflowStdErr
     */
    String getWorkflowStdErr() {
        return workflowStdErr;
    }

    /**
     * @param workflowStdErr the workflowStdErr to set
     */
    void setWorkflowStdErr(String workflowStdErr) {
        this.workflowStdErr = workflowStdErr;
    }

}
