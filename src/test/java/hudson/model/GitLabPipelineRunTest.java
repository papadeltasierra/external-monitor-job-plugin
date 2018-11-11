package hudson.model;

import hudson.EnvVars;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author Kohsuke Kawaguchi
 */
public class GitLabPipelineRunTest extends HudsonTestCase {
    public void test1() throws Exception {
        GitLabPipelineJob p = hudson.createProject(GitLabPipelineJob.class, "test");
        GitLabPipelineRun b = p.newBuild();
        b.acceptRemoteSubmission(new StringReader(
            //"<run><log content-encoding='UTF-8'>AAAAAAAA</log><result>0</result><duration>100</duration></run>"
            "{\"run\": {\"log content-encoding='UTF-8'\":\"AAAAAAAA\"},\"result\":0,\"duration\":100}"
        ));
        assertEquals(b.getResult(),Result.SUCCESS);
        assertEquals(b.getDuration(),100);

        b = p.newBuild();
        b.acceptRemoteSubmission(new StringReader(
            //"<run><log content-encoding='UTF-8'>AAAAAAAA</log><result>1</result>"
            "{\"run\":{\"log content-encoding='UTF-8'\":\"AAAAAAAA\"},\"result\":1}"
        ));
        assertEquals(b.getResult(),Result.FAILURE);
    }

    @Bug(11592)
    public void testGitLabPipelineJob() throws Exception {
        GitLabPipelineJob p = jenkins.createProject(GitLabPipelineJob.class, createUniqueProjectName());
        GitLabPipelineRun b = p.newBuild();
        b.acceptRemoteSubmission(new StringReader(
            //"<run><log content-encoding='UTF-8'></log><result>0</result><duration>1</duration></run>"
            "{\"run\":\"log content-encoding='UTF-8'\",\"result\":0},\"duration\":1}"
        ));

        assertGetEnvironmentWorks(b);
    }

    @SuppressWarnings("rawtypes")
    private void assertGetEnvironmentWorks(Run build) throws IOException, InterruptedException {
        whenJenkinsMasterHasNoExecutors();
        // and getEnvironment is called outside of build
        EnvVars envVars =  build.getEnvironment(StreamTaskListener.fromStdout());
        // then it should still succeed - i.e. no NPE o.s.l.t.
        assertNotNull(envVars);
    }

    private void whenJenkinsMasterHasNoExecutors() throws IOException {
        Jenkins.getInstance().setNumExecutors(0);
        // force update of nodes (TODO https://github.com/jenkinsci/jenkins/pull/1596 renders this workaround unnecessary):
        Jenkins.getInstance().setNodes(Jenkins.getInstance().getNodes());
        // does not reliably work: Assert.assertNull(Jenkins.getInstance().toComputer());
    }
}
