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
import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletResponse;
//import jenkins.model.Jenkins;
//import org.jenkinsci.plugins.gitlabpipelinejob.Messages;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class GitLab implements RootAction, StaplerProxy {
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
        //##PDS should have common/shared function.
        return "gitlab";
    }

    @Override
    public Object getTarget() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req.getRestOfPath().length()==0) {
            if ("GET".equals(req.getMethod())) {
                return this;
            } else {
                throw HttpResponses.forbidden();
            }
        } else {
            return this;
        }
    }

    /*
    public void doPipeline(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        final Jenkins jenkins = Jenkins.getActiveInstance();
        jenkins.checkPermission(Jenkins.READ);

        // Strip trailing slash
        final String commandName = req.getRestOfPath().substring(1);
        CLICommand command = CLICommand.clone(commandName);
        if (command == null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "No such command");
            return;
        }

        req.setAttribute("command", command);
        req.getView(this, "command.jelly").forward(req, rsp);
    }
    */
}