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

//import hudson.Proc;
//import hudson.util.DecodingStream;
//import hudson.util.DualOutputStream;
//import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import java.io.IOException;
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.XMLStreamReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.io.InputStream;
//import java.io.Reader;
//import java.util.zip.GZIPInputStream;
import javax.servlet.ServletException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import jenkins.model.Jenkins;
//import java.hudson.model.Job;
import org.kohsuke.stapler.StaplerRequest;
import java.lang.Object;
import java.lang.String;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static javax.xml.stream.XMLStreamConstants.*;

// ## Where are we capturng failures?

/**
 * {@link Run} for {@link GitLabPipelineJob}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class GitLabWebhookPipeline extends GitLabWebhook {

    private static final Logger LOGGER = Logger.getLogger( GitLabPipelineRun.class.getName() );

    public void process(StaplerRequest req) 
    		throws IOException, JSONObjectException, ServletException, InterruptedException {
        Map<String, Object> pipeline = parseJsonRequest(req);

        // ##TEST Bad Json, all missnig fields..
        // Start by identifying the GitLab project, which maps to a Jenkins Job
        LOGGER.log(Level.INFO, "Parsing JSON");

        // Check this really is a pipeline request.
        // ##TEST non-pippeline job or missing object_kind.
        String jsonObjKind = (String)pipeline.get("object_kind");
        if (jsonObjKind.equals("pipeline")) {
            LOGGER.log(Level.INFO, "Really pipeline event");
            // ##PDS Make all these string attribute names into a enum/class.
            // The ref is either 'master' or a branch name.
            Map<String, Object> jsonProject =
                (Map<String, Object>)pipeline.get("project");
            String name = (String)jsonProject.get("name");

            Map<String, Object> jsonObjAttrs =
                (Map<String, Object>)pipeline.get("object_attributes");
            String ref = (String)jsonObjAttrs.get("ref");

            LOGGER.log(Level.INFO, "GitLab Project name: " + name);
            LOGGER.log(Level.INFO, "GitLab Project ref: " + ref);

            /*
             * We construct the project name from the GitLab project name plus
             * the GitLab master/branch name with many characters such as '/'
             * converted to underscores.
             */
            String project = name + "_" + ref;

            // ##TEST Various string strings
            project = project.replaceAll("[^-0-9a-zA-Z_]", "_");
            LOGGER.log(Level.INFO, "Jenkins Project: " + project);

            /*
             * Now we lookup the project to see if it exists because if not then we
             * can give up and go home.
             */
            // TEST - No matching name, wrong Job type.
            Job gitLabPipelineJob = (Job)Jenkins.getInstance().getItem(project);
            if (gitLabPipelineJob instanceof GitLabPipelineJob) {
                LOGGER.log(Level.INFO, "Found a pipeline job");
                GitLabPipelineJob glpj = (GitLabPipelineJob)gitLabPipelineJob;
                glpj.pipelineRequest(pipeline);
            }
            // Find this job.
        } else {
            LOGGER.log(Level.WARNING, "Unexpected object_kind: " + jsonObjKind);
        }
    }
}
