/*
 * The MIT License
 *
 * Copyright 2014 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.durabletask;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.util.LogTaskListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Defines how to control the execution of a task after it has started.
 * Expected to be XStream and Java serializable.
 */
public abstract class Controller implements Serializable {

    /**
     * Obtains any new task log output.
     * Could use a serializable field to keep track of how much output has been previously written.
     * @param workspace the workspace in use
     * @param sink where to send new log output
     * @return true if something was written and the controller should be resaved, false if everything is idle
     */
    public abstract boolean writeLog(FilePath workspace, OutputStream sink) throws IOException, InterruptedException;

    /**
     * Checks whether the task has finished.
     * @param workspace the workspace in use
     * @param launcher a way to start processes
     * @return an exit code (zero is successful), or null if the task appears to still be running
     */
    public @CheckForNull Integer exitStatus(FilePath workspace, Launcher launcher) throws IOException, InterruptedException {
        if (Util.isOverridden(Controller.class, getClass(), "exitStatus", FilePath.class)) {
            return exitStatus(workspace);
        } else {
            throw new AbstractMethodError("implement exitStatus(FilePath, Launcher)");
        }
    }

    /** @deprecated use {@link #exitStatus(FilePath, Launcher)} instead */
    public @CheckForNull Integer exitStatus(FilePath workspace) throws IOException, InterruptedException {
        return exitStatus(workspace, createLauncher(workspace));
    }

    /**
     * Obtain the process output.
     * Intended for use after {@link #exitStatus(FilePath, Launcher)} has returned a non-null status.
     * The result is undefined if {@link DurableTask#captureOutput} was not called before launch; generally an {@link IOException} will result.
     * @param workspace the workspace in use
     * @param launcher a way to start processes
     * @return the output of the process as raw bytes (may be empty but not null)
     */
    public @Nonnull byte[] getOutput(@Nonnull FilePath workspace, @Nonnull Launcher launcher) throws IOException, InterruptedException {
        throw new IOException("Did not implement getOutput in " + getClass().getName());
    }

    /**
     * Tries to stop any running task.
     * @param workspace the workspace in use
     * @param launcher a way to start processes
     */
    public void stop(FilePath workspace, Launcher launcher) throws IOException, InterruptedException {
        if (Util.isOverridden(Controller.class, getClass(), "stop", FilePath.class)) {
            stop(workspace);
        } else {
            throw new AbstractMethodError("implement stop(FilePath, Launcher)");
        }
    }

    /** @deprecated use {@link #stop(FilePath, Launcher)} instead */
    public void stop(FilePath workspace) throws IOException, InterruptedException {
        stop(workspace, createLauncher(workspace));
    }

    private static Launcher createLauncher(FilePath workspace) throws IOException, InterruptedException {
        return workspace.createLauncher(new LogTaskListener(Logger.getLogger(Controller.class.getName()), Level.FINE));
    }

    /**
     * Cleans up after a task is done.
     * Should delete any temporary files created by {@link DurableTask#launch}.
     * @param workspace the workspace in use
     */
    public abstract void cleanup(FilePath workspace) throws IOException, InterruptedException;

    /**
     * Should be overridden to provide specific information about the status of an external process, for diagnostic purposes.
     * @return {@link #toString} by default
     */
    public String getDiagnostics(FilePath workspace, Launcher launcher) throws IOException, InterruptedException {
        return toString();
    }

    private static final long serialVersionUID = 1L;
}
