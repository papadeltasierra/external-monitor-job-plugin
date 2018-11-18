/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.model;

import hudson.Proc;
import hudson.Util;
import hudson.util.DecodingStream;
import hudson.util.DualOutputStream;
import hudson.util.TimeUnit2;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.kohsuke.stapler.export.Exported;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * {@link Run} for {@link GitLabPipelineJob}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class GitLabPipelineRun extends Run<GitLabPipelineJob,GitLabPipelineRun> {
    /**
     * Loads a run from a log file.
     * @param owner
     * @param runDir
     */

    private static final Logger LOGGER = Logger.getLogger( GitLabPipelineRun.class.getName() );
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private String commit;
    private Executor executor;
    /* 
     * Hook the delete method and make sure that we remove ourselves from the
     * project job's commit Map.
     */
    public void delete() 
            throws java.io.IOException {
        super.delete();
        if (commit != null) {
            project.commitMap.remove(commit);
        }
    }

    /**
     * Loads a run from a log file.
     * @param owner
     * @param runDir
     */
    GitLabPipelineRun(GitLabPipelineJob owner, File runDir) throws IOException {
        super(owner,runDir);
    }

    GitLabPipelineRun(GitLabPipelineJob owner, String commit) 
            throws java.io.IOException {
        super(owner);
        LOGGER.log(Level.INFO, "Creating run for: " + commit);
        this.commit = commit;
        LOGGER.log(Level.INFO, "DEBUG: timestamp: " + timestamp);
        owner.commitMap.put(commit, this);
    }

    /**
     * Instead of performing a build, run the specified command,
     * record the log and its exit code, then call it a build.
     * @param cmd   command to run as a build
     */


    // ## PDS We need to override run() and execute() and stop them doing nasty things!
    //protected final void execute(@Nonnull RunExecution job) {
    //}

    // ##PDS Make sure that nobody can 'build' our job manually.

    /*
     * Process the received Pipeline request.  The internal status of the run
     * cannot be set so we have only the Result that we can update.
     * We also handle appearing to 'jump in' at the start of a job.
     */ 
    public void process(final Map<String, Object> jsonReq) {
        //## TEST status / details_status d not exist.
        LOGGER.log(Level.INFO, "Process status change");
        Map<String, Object> jsonObjAttrs = (Map<String, Object>)jsonReq.get("object_attributes");
        String status = (String)jsonObjAttrs.get("status");
        String details_status = (String)jsonObjAttrs.get("detailed_status");
        Result newResult = result;


        // ##TEST What happens if we restart a completed build?
        // ##PDS What is we never see the endpoint? We need a configurable
        //       timeout.
        // ##PDS What happens if someone manually 'stops' the build from
        //       Jenkins?  Should that be possible?
        //       It could be: https://docs.gitlab.com/ee/api/pipelines.html
        //       We need a secret token.
        /*
         * Reference: https://github.com/gitlabhq/gitlabhq
         *     files: gitlabhq/spec/lib/gitlab/ci/status...
         */
        if (status.equals(GitLabStatus.MANUAL) ||
            status.equals(GitLabStatus.CREATED) ||
            status.equals(GitLabStatus.SCHEDULED)) {
            /* 
             * We ignore these statuses because we don't have a way to signal
             * on Jenkins why we were started - we just know that we were.
             */
            LOGGER.log(Level.INFO, "Ignore status change [1]: " + status);
        } else if (status.equals(GitLabStatus.FACTORY) ||
                   status.equals(GitLabStatus.EXTENDED)) {
            /*
             * Not at all clear what these mean!
             */
            LOGGER.log(Level.INFO, "Ignore status change [2]: " + status);
        } else if (status.equals(GitLabStatus.PENDING)) {
            LOGGER.log(Level.INFO, "Pending...");
        } else if (status.equals(GitLabStatus.RUNNING)) {
            LOGGER.log(Level.INFO, "Running...");
            onStartBuilding();
        } else if (status.equals(GitLabStatus.SKIPPED)) {
            LOGGER.log(Level.INFO, "Skipped!");
            newResult = Result.NOT_BUILT;
        } else if (status.equals(GitLabStatus.CANCELED)) {
            LOGGER.log(Level.INFO, "Canceled!");
            newResult = Result.ABORTED;
        } else if (status.equals(GitLabStatus.FAILED)) {
            LOGGER.log(Level.INFO, "Failed!");
            newResult = Result.FAILURE;
        } else if (status.equals(GitLabStatus.SUCCESS)) {
            LOGGER.log(Level.INFO, "Success");
            newResult = Result.SUCCESS;
            if (details_status.equals(GitLabStatus.SUCCESS_WARNING)) {
                LOGGER.log(Level.INFO, "...with warnings!");
                newResult = Result.UNSTABLE;
            }
        } else if (status.equals(GitLabStatus.SUCCESS_WARNING)) {
            LOGGER.log(Level.INFO, "Success-with-warnings");
            newResult = Result.UNSTABLE;
        } else {
            LOGGER.log(Level.WARNING, "Unknown pipeline status: " + status);
        }

        // ##PDS We are not setting/calculating start time estimated end 
        //       times so the dashboard won't function!
        // ##PDS It would also be nice to have some log output even if only
        //       indicating when we received updates from GitLab.

        if (newResult != result) {
            LOGGER.log(Level.INFO, "Result state change: " + 
                String.valueOf(result) + " to " + String.valueOf(newResult));
            if (isBuilding() && !isInProgress()) {
                LOGGER.log(Level.INFO, "Fake build start");
                onStartBuilding();
            }          
            setResult(newResult);

            /*
             * Calculate the duration because this is the key thing that
             * allows dashboards to show estimated progress.
             */
            long end = System.currentTimeMillis();
            duration = Math.max(end - getStartTimeInMillis(), 0);

            /* 
             * Save the build record otherwise future builds won't find this
             * build and won't be able to figure out estimated completion
             * times.
             */
            try {
                    save();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to save build record",e);
                } 
            LOGGER.log(Level.INFO,
                "Build started at: " + getStartTimeInMillis());
            LOGGER.log(Level.INFO, "Build duration: " + duration);
            onEndBuilding();
        }
    }

    public @CheckForNull Executor getExecutor() {
        return executor;
    }

    /*************************************************************************
     * The following is essentially copied from Executor.java.
     ************************************************************************/
    public long getElapsedTime() {
        lock.readLock().lock();
        try {
            return System.currentTimeMillis() - getStartTimeInMillis();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the string that says how long since this build has started.
     *
     * @return
     *      string like "3 minutes" "1 day" etc.
     */
    public String getTimestampString() {
        return Util.getPastTimeString(getElapsedTime());
    }

        /**
     * Returns the progress of the current build in the number between 0-100.
     *
     * @return -1
     *      if it's impossible to estimate the progress.
     */
    @Exported
    public int getProgress() {
        long d;
        lock.readLock().lock();
        try {
            d = project.getEstimatedDuration();
        } finally {
            lock.readLock().unlock();
        }
        if (d <= 0) {
            return -1;
        }

        int num = (int) (getElapsedTime() * 100 / d);
        if (num >= 100) {
            num = 99;
        }
        return num;
    }

    /**
     * Returns true if the current build is likely stuck.
     *
     * <p>
     * This is a heuristics based approach, but if the build is suspiciously taking for a long time,
     * this method returns true.
     */
    @Exported
    public boolean isLikelyStuck() {
        long d;
        long elapsed;
        lock.readLock().lock();
        try {
            elapsed = getElapsedTime();
            d = project.getEstimatedDuration();
        } finally {
            lock.readLock().unlock();
        }
        if (d >= 0) {
            // if it's taking 10 times longer than ETA, consider it stuck
            return d * 10 < elapsed;
        } else {
            // if no ETA is available, a build taking longer than a day is considered stuck
            return TimeUnit2.MILLISECONDS.toHours(elapsed) > 24;
        }
    }
}
