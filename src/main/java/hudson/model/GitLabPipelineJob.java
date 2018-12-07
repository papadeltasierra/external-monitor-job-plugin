/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, id:cactusman
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

import hudson.Extension;
import hudson.model.RunMap.Constructor;
import hudson.util.AlternativeUiTextProvider;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.gitlabpipelinejob.Messages;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;

//##PDS debug only
import hudson.model.PermalinkProjectAction.Permalink;
import java.lang.Boolean;
import java.lang.Integer;

/**
 * Job that runs outside Hudson whose result is submitted to Hudson
 * (either via web interface, or simply by placing files on the file system,
 * for compatibility.)
 *
 * @author Kohsuke Kawaguchi
 */
public class GitLabPipelineJob extends ViewJob<GitLabPipelineJob,GitLabPipelineRun> implements TopLevelItem {
//public class GitLabPipelineJob extends Job<GitLabPipelineJob,GitLabPipelineRun> implements TopLevelItem {

    private static final Logger LOGGER = Logger.getLogger( GitLabPipelineRun.class.getName() );

    /*
     * Map to allow us to find runs by GitLab commit ids.
     */
    // ##PDS accessors to add/delete us?
    public Map<String, GitLabPipelineRun> commitMap;

    /*
     * Although we don't truly execute anything, we need to have a fake
     * Executor because that is what Jenkis looks to in order to get progress
     * information.
     */
    private Executor executor;

    public GitLabPipelineJob(String name) {
        this(Jenkins.getInstance(),name);
        LOGGER.log(Level.INFO, "Create job by name");
    }

    /**
     * @deprecated as of 1.390
     */
    @Deprecated
    protected GitLabPipelineJob(Jenkins parent, String name) {
        super(parent,name);
        LOGGER.log(Level.INFO, "Creating commitMap");
        commitMap = new HashMap();
        LOGGER.log(Level.INFO, "this: " + this.toString());
        LOGGER.log(Level.INFO, "commitMap: " + commitMap.toString());
    }

    public GitLabPipelineJob(ItemGroup parent, String name) {
        super(parent, name);
        LOGGER.log(Level.INFO, "Creating commitMap");
        commitMap = new HashMap();
        LOGGER.log(Level.INFO, "this: " + this.toString());
        LOGGER.log(Level.INFO, "commitMap: " + commitMap.toString());
    }

    // ##PDS Debugging.
    //    @Exported
    //@QuickSilver
    public GitLabPipelineRun  getLastSuccessfulBuild() {
        //return (RunT)Permalink.LAST_SUCCESSFUL_BUILD.resolve(this);
        GitLabPipelineRun run = (GitLabPipelineRun)Permalink.LAST_BUILD.resolve(this);
        if (run == null) {
            LOGGER.log(Level.INFO, "last_build: " + run.toString());
            LOGGER.log(Level.INFO, "isBuilding: " + (new Boolean(run.isBuilding())).toString());
            LOGGER.log(Level.INFO, "getResult: " + run.getResult().toString());
            LOGGER.log(Level.INFO, "isBetter: " + (new Boolean(run.getResult().isBetterOrEqualTo(Result.UNSTABLE))).toString());
        } else {
            LOGGER.log(Level.INFO, "!!! last run not found");
        }
        return run;
    }


    /* ##PDS
     * We will want to extend this if we allow a back-channel to cancel
     * the job.
     */
    private GitLabPipelineRun findOrCreateRun(String commit)
            throws IOException {

        // ##PDS Do we need to worry about threading access here?
        // ##TEST Find existing run, create new run, run has completed.
        GitLabPipelineRun run = this.commitMap.get(commit);
        if ((run != null) && (!run.isLogUpdated())) {
            LOGGER.log(Level.INFO, "Found run has completed");
            /*
             * If the run has completed, then this is probably a 'restart'
             * so we create a new Jenkins build and map the commit to this
             * new build.
             */
            run = null;
        }

        if (run == null) {
            // Get a new run.
            LOGGER.log(Level.INFO, "Create a new run");
            run = new GitLabPipelineRun(this, commit);
            /* 
             * These update internal state and add the new run into the list
             * of runs that are visible on the dashboards.
             */
            _getRuns();
            runs.put(run);
        }
        return run;
    }

    /*
     * Process a received pipeline request.
     */
    public void pipelineRequest(Map<String, Object> jsonReq)
            throws java.io.IOException, ServletException, InterruptedException {
        LOGGER.log(Level.INFO, "Job sees pipeline request");

        // ##  TEST Test this.
        checkPermission(AbstractProject.BUILD);

        LOGGER.log(Level.INFO, "Build is permitted");
        // Extract the commit ID and look for an existing run.
        // ##TEST missing obj_attrs, sha
        Map<String, Object> jsonObjAttrs = 
            (Map<String, Object>)jsonReq.get("object_attributes");
        LOGGER.log(Level.INFO, "jsonObjAttrs: " + jsonObjAttrs.toString());
        String commit = (String)jsonObjAttrs.get("sha");
        LOGGER.log(Level.INFO, "commit: " + commit);
        LOGGER.log(Level.INFO, "this: " + this.toString());
        LOGGER.log(Level.INFO, "commitMap: " + this.commitMap.toString());

        GitLabPipelineRun run = findOrCreateRun(commit);
        run.process(jsonReq);
    }


    public TopLevelItemDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final TopLevelItemDescriptor DESCRIPTOR = new DescriptorImpl();

    @Override
    public String getPronoun() {
        return AlternativeUiTextProvider.get(PRONOUN, this, Messages.GitLabPipelineJob_DisplayName());
    }

    // ##PDS DEBUGGING - Called from Run...
    /*
     * We cannot use the inherited implementation of getEstimatedduration
     * because this is tightly integrated with PERMALINKS and RunListeners
     * and these come with far too much baggage.  So we have to duplicate
     * that code here but done in a 'slow way' of just walking back through
     * previous builds.
     *
     * Returns candidate build for calculating the estimated duration of the current run.
     * 
     * Returns the 3 last successful (stable or unstable) builds, if there are any.
     * Failing to find 3 of those, it will return up to 3 last unsuccessful builds.
     * 
     * In any case it will not go more than 6 builds into the past to avoid costly build loading.
     */
    @SuppressWarnings("unchecked")
    protected List<GitLabPipelineRun> getEstimatedDurationCandidates() {

        int i = 0;
        int lastBuildNumber = -1;
        GitLabPipelineRun r = getLastBuild();       
        List<GitLabPipelineRun> candidates = new ArrayList<GitLabPipelineRun>(3);
        List<GitLabPipelineRun> fallbackCandidates = new ArrayList<GitLabPipelineRun>(3);
        if (r != null) {
            lastBuildNumber = r.getNumber();
            do {
                if (!r.isBuilding() && r.getResult() != null) {
                    Result result = r.getResult();
                    if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
                        LOGGER.log(Level.INFO, "Good one");
                        candidates.add(r);
                    } else if (result.isCompleteBuild()) {
                        LOGGER.log(Level.INFO, "Fallback");
                        fallbackCandidates.add(r);
                    }
                }
                i++;
                r = r.getPreviousBuild();
            } while (r != null && r.getNumber() != lastBuildNumber && candidates.size() < 3 && i < 6);
        }
        
        while (candidates.size() < 3) {
            if (fallbackCandidates.isEmpty())
                break;
            GitLabPipelineRun run = fallbackCandidates.remove(0);
            candidates.add(run);
        }
        LOGGER.log(Level.INFO, "# of candidates: " + candidates.size());

        return candidates;
    }
    
    public long getEstimatedDuration() {
        List<GitLabPipelineRun> builds = getEstimatedDurationCandidates();
        
        if(builds.isEmpty())     return -1;

        long totalDuration = 0;
        for (GitLabPipelineRun b : builds) {
            totalDuration += b.getDuration();
        }
        if(totalDuration==0) return -1;

        return Math.round((double)totalDuration / builds.size());
    }



    // ##PDS Can we delete using HTTP etc?  If so then we need to configure...
    //       - GitLab job ID (or name/branch)
    //       - URL to point at
    //       -- possible security string.
    /*
     * For now, disable the 'cancel X' becuase we can't cancel GitLab jobs.
     */
    public boolean hasAbortPermission() {
        LOGGER.log(Level.INFO, "*** DEBUG *** No abort permission");
        return false;
    }

    public static final class DescriptorImpl extends TopLevelItemDescriptor {


        public String getDisplayName() {
            return Messages.GitLabPipelineJob_DisplayName();
        }

        /**
         * Needed if it wants External Monitor Jobs are categorized in Jenkins 2.x.
         *
         * TODO: Override when the baseline is upgraded to 2.x
         *
         * @return A string it represents a ItemCategory identifier.
         */
        public String getCategoryId() {
            return "standalone-projects";
        }

        /**
         * Needed if it wants External Monitor Jobs are categorized in Jenkins 2.x.
         *
         * TODO: Override when the baseline is upgraded to 2.x
         *
         * @return A string with the Item description.
         */
        public String getDescription() {
            return Messages.GitLabPipelineJob_Description();
        }

        /**
         * Needed if it wants External Monitor Jobs are categorized in Jenkins 2.x.
         *
         * TODO: Override when the baseline is upgraded to 2.x
         *
         * @return A string it represents a URL pattern to get the Item icon in different sizes.
         */
        public String getIconFilePathPattern() {
            return "plugin/external-monitor-job/images/:size/gitlabpipelinejob.png";
        }

        public GitLabPipelineJob newInstance(ItemGroup parent, String name) {
            return new GitLabPipelineJob(parent, name);
        }
    }

    /**
     * Reloads the list of {@link Run}s. This operation can take a long time.
     *
     * <p>
     * The loaded {@link Run}s should be set to {@link #runs}.
     */
    @Override
    protected void reload() {
        this.runs.load(this,new Constructor<GitLabPipelineRun>() {
            public GitLabPipelineRun create(File dir) throws IOException {
                return new GitLabPipelineRun(GitLabPipelineJob.this,dir);
            }
        });
    }
}
