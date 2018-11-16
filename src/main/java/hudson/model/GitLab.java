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
import hudson.ExtensionPoint;
//import hudson.model.RunMap.Constructor;
//import hudson.util.AlternativeUiTextProvider;
//import java.io.File;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.logging.Logger;
import java.util.logging.Level;

//import javax.servlet.http.HttpServletResponse;
//import jenkins.model.Jenkins;
//import org.jenkinsci.plugins.gitlabpipelinejob.Messages;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

// ##PDS Add logging, especially of errors.

@Extension
public class GitLab implements RootAction, StaplerProxy {

    private static final Logger LOGGER = Logger.getLogger( GitLab.class.getName() );

    private static final String URL_NAME="gitlab";

    // ##PDS just a hook for logging.
    void GitLab() {
        LOGGER.setLevel(Level.INFO);
    }

    /*
     * Return the icon that gets displayed when the .../gitlab page is displayed.
     */
    public String getIconFileName() {
        return null;
    }

    /*
     * This is not an action that appears in the Jenkins menus.
     */
    public String getDisplayName() {
        return null;
    }

    /* 
     * The relative position below the Jenkins root.
     */
    public String getUrlName() {
        return "gitlab";
    }

    public void doPipeline(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {

        LOGGER.log(Level.INFO, "Handling Pipeline event");        
        // ##PDS Try responding imediately here!
        // rsp.sendError(200);
        new GitLabWebhookPipeline().process(req);

        //req.getView(this, "pipeline.jelly").forward(req, rsp);
        //rsp.sendError(444, "Bog off!");
        //Do nothing and a 200 response results.
    }

    /*
     * As required, these functions will support the other GitLab webhook
     * events.  For now we do nothing which results in a 200 response
     * being returned.
     */
    public void doPush(StaplerRequest req, StaplerResponse rsp)  
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "Push event ignored");
    }

    public void doTag(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "Tag event ignored");
    }

    public void doComment(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "Comment event ignored");
    }

    public void doConfidentialComment(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "Confidential Comment event ignored");
    }

    public void doIssues(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "Issues event ignored");
    }

    public void doMergeRequest(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "Merge Request event ignored");
    }

    public void doWikiPage(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "WiKi Page event ignored");
    }

    public void doBuild(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        // ##PDS Wonder what this does and should we care?        
        LOGGER.log(Level.INFO, "Build event ignored");
    }

    @Override
    public Object getTarget() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req.getRestOfPath().length()!=0) {
            if ("POST".equals(req.getMethod())) {
                // Pass long to the appropriate handler for the next page.
                return this;
            } else {
                // ##PDS change this for languages.
                throw HttpResponses.errorWithoutStack(403, "POST method only!");
            }
        } else {
            // This causes us to return the webpage defined in resources.
            return this;
        }
    }
}