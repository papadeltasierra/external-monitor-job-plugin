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
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.XMLStreamReader;
//import java.io.File;
import java.io.IOException;
//import java.io.PrintStream;
//import java.io.InputStream;
//import java.io.Reader;
//import java.util.zip.GZIPInputStream;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.kohsuke.stapler.StaplerRequest;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * {@link Run} for {@link GitLabPipelineJob}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class GitLabWebhook {

    private static final Logger LOGGER = Logger.getLogger( GitLabPipelineRun.class.getName() );

    /*
     * Parse the JSON contained in the request into an object.
     */
    protected Map<String, Object> parseJsonRequest(StaplerRequest req) 
        throws IOException, JSONObjectException {

        LOGGER.log(Level.INFO, "Parsing received JSON");
        Map<String, Object> jsonEvent = JSON.std.mapFrom(req.getReader());

        LOGGER.log(Level.INFO, "JSON parsed OK");
        return jsonEvent;
    }
}
