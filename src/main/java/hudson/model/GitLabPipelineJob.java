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

/**
 * Job that runs outside Hudson whose result is submitted to Hudson
 * (either via web interface, or simply by placing files on the file system,
 * for compatibility.)
 *
 * @author Kohsuke Kawaguchi
 */
public class GitLabPipelineJob extends ViewJob<GitLabPipelineJob,GitLabPipelineRun> implements TopLevelItem {

    private static final Logger LOGGER = Logger.getLogger( GitLabPipelineRun.class.getName() );

    /*
     * Map to allow us to find runs by GitLab commit ids.
     */
    // ##PDS accessors to add/delete us?
    public Map<String, GitLabPipelineRun> commitMap;

    // ##PDS I think we can remove these now we know how DisplayName works.
    /*
     * Override default storage and methods which use 'name'.
     */
    /*
    private String displayName;

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    */
    /**
     * This is intended to be used by the Job configuration pages where
     * we want to return null if the display name is not set.
     * @return The display name of this object or null if the display name is not
     * set
     *//*
    public String getDisplayNameOrNull() {
        return displayName;
    }
    */
    public GitLabPipelineJob(String name) {
        this(Jenkins.getInstance(),name);
    }

    public GitLabPipelineJob(ItemGroup parent, String name) {
        super(parent, name);
        LOGGER.log(Level.INFO, "Creating commitMap");
        commitMap = new HashMap();
        LOGGER.log(Level.INFO, "this: " + this.toString());
        LOGGER.log(Level.INFO, "commitMap: " + commitMap.toString());
    }

    /*
     * Process a received pipeline request.
     */
    public void pipelineRequest(Map<String, Object> jsonReq)
            throws java.io.IOException {
        LOGGER.log(Level.INFO, "Job sees pipeline request");
        // Extract the commit ID and look for an existing run.
        // ##TEST missing obj_attrs, sha
        Map<String, Object> jsonObjAttrs = 
            (Map<String, Object>)jsonReq.get("object_attributes");
        LOGGER.log(Level.INFO, "jsonObjAttrs: " + jsonObjAttrs.toString());
        String commit = (String)jsonObjAttrs.get("sha");
        LOGGER.log(Level.INFO, "commit: " + commit);
        LOGGER.log(Level.INFO, "this: " + this.toString());
        LOGGER.log(Level.INFO, "commitMap: " + this.commitMap.toString());

        // ##PDS Do we need to worry abuot threading access here?
        // ##TEST Find exiting run, create new run.
        GitLabPipelineRun run = this.commitMap.get(commit);
        if (run == null) {
            // Get a new run.
            LOGGER.log(Level.INFO, "This is a new job");
            run = new GitLabPipelineRun(this, commit);
            /* 
             * I believe that this updates some internal state as there is no
             * other reason to call this and ignore the result!
             */
            _getRuns();
        }
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
