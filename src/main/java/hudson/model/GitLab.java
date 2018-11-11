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
//import java.io.IOException;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletResponse;
//import jenkins.model.Jenkins;
//import org.jenkinsci.plugins.gitlabpipelinejob.Messages;
import org.kohsuke.stapler.StaplerProxy;
//import org.kohsuke.stapler.StaplerRequest;
//import org.kohsuke.stapler.StaplerResponse;

@Extension
public class GitLab implements ExtensionPoint, RootAction, StaplerProxy {
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "GitLab";
    }

    public String getUrlName() {
        return "gitlab";
    }

    public void doApi() {

    }
}