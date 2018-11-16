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
import hudson.util.DecodingStream;
import hudson.util.DualOutputStream;
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

    private String commit;
    /* 
     * Hook the delete method and make sure that we remove ourselves from the
     * parent job's commit Map.
     */
    public void delete() 
            throws java.io.IOException {
        super.delete();
        if (commit != null) {
            project.commitMap.remove(commit);
        }
    }

    GitLabPipelineRun(GitLabPipelineJob owner, String commit) 
            throws java.io.IOException {
        super(owner);
        LOGGER.log(Level.INFO, "Creating run for: " + commit);
        this.commit = commit;
        timestamp = System.currentTimeMillis();
        owner.commitMap.put(commit, this);
        // Done in superclass.  setResult(State.NOT_STARTED);
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

    // ##PDS Call setResult() to update results but we have to change to State.BUILDING 
    // ##    via onStartBuilding().
    // ##    State.NOT_STARTED, BUIDING, POST_PRODUCTION (only for real builds), COMPLETED.

    // ## Set results using setResult (must be building).
    // Result.ABORTED, FAILURE, NOT_BUILT, UNSTABLE (there were non-fatal error), SUCCESS

    /*
     * Process the recieved Pipeline request.
     */ 
    public void process(final Map<String, Object> jsonReq) {
        //## TEST status / details_status d not exist.
        LOGGER.log(Level.INFO, "Process status change");
        Map<String, Object> jsonObjAttrs = (Map<String, Object>)jsonReq.get("object_attributes");
        String status = (String)jsonObjAttrs.get("status");
        String details_status = (String)jsonObjAttrs.get("detailed_status");

        if (status.equals(GitLabStatus.MANUAL) ||
            status.equals(GitLabStatus.CREATED) ||
            status.equals(GitLabStatus.SCHEDULED)) {
            /* 
             * We ignore these statuses because we don't have a way to signal
             * on Jenkins why we were started - we just know thatwe were.
             */
            LOGGER.log(Level.INFO, "Ignore status change: " + status);
        } else if (status.equals(GitLabStatus.RUNNING)) {
            LOGGER.log(Level.INFO, "Running...");
            onStartBuilding();
        } else if (status.equals(GitLabStatus.SKIPPED)) {
            LOGGER.log(Level.INFO, "Skipped!");
            setResult(Result.NOT_BUILT);
            onEndBuilding();
        } else if (status.equals(GitLabStatus.CANCELED)) {
            LOGGER.log(Level.INFO, "Canceled!");
            setResult(Result.ABORTED);
            onEndBuilding();
        } else if (status.equals(GitLabStatus.FAILED)) {
            LOGGER.log(Level.INFO, "Failed!");
            setResult(Result.FAILURE);
            onEndBuilding();
        } else if (status.equals(GitLabStatus.SUCCESS)) {
            LOGGER.log(Level.INFO, "Success");
            setResult(Result.SUCCESS);
            if (details_status.equals(GitLabStatus.SUCCESS_WARNING)) {
                LOGGER.log(Level.INFO, "...with warnings!");
                setResult(Result.UNSTABLE);
            }
            onEndBuilding();
        } else if (status.equals(GitLabStatus.SUCCESS_WARNING)) {
            LOGGER.log(Level.INFO, "Success-with-warnings");
            onEndBuilding();
        } else {
            LOGGER.log(Level.WARNING, "Unknown pipeline status: " + status);
        }
    }
}
