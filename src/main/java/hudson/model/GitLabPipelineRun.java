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

    GitLabPipelineRun(GitLabPipelineJob owner, File runDir) throws IOException {
        super(owner,runDir);
    }

    /**
     * Creates a new run.
     * @param project
     */
    GitLabPipelineRun(GitLabPipelineJob project) throws IOException {
        super(project);
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


    public void run(final String[] cmd) {
        execute(new RunExecution() {
            public Result run(BuildListener listener) throws Exception {
                Proc proc = new Proc.LocalProc(cmd, getEnvironment(listener), System.in, new DualOutputStream(System.out, listener.getLogger()));
                return proc.join() ==0 ? Result.SUCCESS : Result.FAILURE;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });
    }




    private void setCharset(String c) { // JENKINS-14107
        charset = c;
    }

    /**
     * Instead of performing a build, accept the log and the return code
     * from a remote machine.
     *
     * <p>
     * The format of the XML is:
     *
     * {@code <pre><xmp>
     * <run>
     *  <log>...console output...</log>
     *  <result>exit code</result>
     * </run>
     * </xmp></pre>}
     *
     * @param in    Log file referenc
     * @throws IOException
     */
    @SuppressWarnings({"Since15"})
    @IgnoreJRERequirement
    public void acceptRemoteSubmission(final Reader in) throws IOException {
        final long[] duration = new long[1];
        execute(new RunExecution() {
            @SuppressFBWarnings(value = {"OS_OPEN_STREAM", "DM_DEFAULT_ENCODING"}, justification = "Logger will be handled upstream")
            public Result run(BuildListener listener) throws IOException, JSONObjectException {

                LOGGER.log(Level.INFO, "processing received JSON");

                //##PDS what was this for?
                //PrintStream logger = new PrintStream(new DecodingStream(listener.getLogger()));

                LOGGER.log(Level.INFO, "received JSON 1");
                Map<String, Object> jsonEvent = JSON.std.mapFrom(in);
                LOGGER.log(Level.INFO, "received JSON 2");
                //LOGGER.log(Level.FINER, "received JSON:\n{0}", jsonEvent.toString());

                //p.nextTag();    // get to the <run>
                //p.nextTag();    // get to the <log>

                //setCharset(p.getAttributeValue(null,"content-encoding"));
                //while (p.next() != END_ELEMENT) {
                //    int type = p.getEventType();
                //    if (type == CHARACTERS || type == CDATA) {
                //        logger.print(p.getText());
                //    }
                //}
                //p.nextTag(); // get to <result>

                LOGGER.log(Level.FINEST, "A finest log");
                LOGGER.log(Level.INFO, "result? " + jsonEvent.toString());
                LOGGER.log(Level.INFO, "result? " + jsonEvent.containsKey("result"));
                LOGGER.log(Level.INFO, "result? " + jsonEvent.get("result").toString());
                //LOGGER.log(Level.FINEST, "result? {0}", new Object[]{jsonEvent.get("result")});
                Result r = (Integer)jsonEvent.get("result") == 0 ? Result.SUCCESS : Result.FAILURE;

                //##PDS Allow for optionals.
                if (jsonEvent.containsKey("duration")) {
                    LOGGER.log(Level.INFO, "duration present: " + jsonEvent.get("duration").getClass().getName().toString());
                    LOGGER.log(Level.INFO, "duration present: " + jsonEvent.get("duration").toString());
                    Integer durInt = (Integer)jsonEvent.get("duration");
                    duration[0] = durInt.longValue();
                }
                if (jsonEvent.containsKey("displayName")) {
                    LOGGER.log(Level.INFO, "displayName present: " + jsonEvent.get("displayName").toString());
                    setDisplayName(jsonEvent.get("displayName").toString());
                }
                if (jsonEvent.containsKey("description")) {
                    LOGGER.log(Level.INFO, "description present");
                    setDescription(jsonEvent.get("description").toString());
                }

                LOGGER.log(Level.INFO, "JSON - done");
                return r;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });

        if(duration[0]!=0) {
            super.duration = duration[0];
            // save the updated duration
            save();
        }
    }

    /**
     * @param result    Result code of the external job
     * @param duration  Duration (in milliseconds) of the external job run
     * @param stream    Stream of external job log
     * @throws IOException
     */
    public void acceptRemoteSubmission(final int result, final long duration, final InputStream stream) throws IOException {
        execute(new RunExecution() {
            @SuppressFBWarnings(value = {"OS_OPEN_STREAM", "DM_DEFAULT_ENCODING"}, justification = "Logger will be handled upstream")
            public Result run(BuildListener listener) throws Exception {
                PrintStream logger = new PrintStream(listener.getLogger());
                final int sChunk = 8192;
                GZIPInputStream zipin = new GZIPInputStream(stream);
                byte[] buffer = new byte[sChunk];
                int length;
                while ((length = zipin.read(buffer, 0, sChunk)) != -1) {
                    logger.write(buffer, 0, length);
                }
                Result r = result == 0 ? Result.SUCCESS : Result.FAILURE;
                return r;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });

        super.duration = duration;
        save();
    }

    /**
     * @param result    Result code of the external job
     * @param duration  Duration (in milliseconds) of the external job run
     * @param log       External job log
     * @throws IOException
     */
    public void acceptRemoteSubmission(final int result, final long duration, final String log) throws IOException {
        execute(new RunExecution() {
            @SuppressFBWarnings(value = {"OS_OPEN_STREAM", "DM_DEFAULT_ENCODING"}, justification = "Logger will be handled upstream")
            public Result run(BuildListener listener) throws Exception {
                PrintStream logger = new PrintStream(listener.getLogger());
                logger.print(log);
                Result r = result == 0 ? Result.SUCCESS : Result.FAILURE;
                return r;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });

        super.duration = duration;
        save();
    }

}
