package io.github.gaol.git_rev_missing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MissCommitsTest {

    private static final Log logger = LogFactory.getLog("g_r_m.impl.test");

    @Test
    public void testNoMissing() throws Exception {
        MissingCommit missingCommit = testGitHub("ihomeland/prtest", "1.0.0", "1.0.2");
        if (missingCommit != null) {
            Assert.assertTrue(missingCommit.isClean());
        }
    }

    @Test
    public void testOneMissing() throws Exception {
        // https://github.com/ihomeland/prtest, revA, revB, revC are used for the testing
        MissingCommit missingCommit = testGitHub("ihomeland/prtest", "revA", "revB");
        if (missingCommit != null) {
            Assert.assertFalse(missingCommit.isClean());
            List<CommitInfo> commits = missingCommit.getCommits();
            Assert.assertEquals(1, commits.size());
            CommitInfo commitInfo = commits.get(0);
            Assert.assertEquals("6a7d8dd7fae154653d04b5c0ca6184b3bd40c107", commitInfo.getCommit().getSha());
        }
    }

    @Test
    public void testNoMissingRebased() throws Exception {
        // revC branch has the commit rebased from revA, so that the sha1 is different, but content is the same
        MissingCommit missingCommit = testGitHub("ihomeland/prtest", "revA", "revC");
        if (missingCommit != null) {
            Assert.assertTrue(missingCommit.isClean());
        }
    }

    @Test
    public void testSameCommitDifferentMessage() throws Exception {
        logger.info("TODO: Add test to test that commit messages not the same(similar), but the content is the same");
    }

    @Test
    public void testSameCommitDifferentLocation() throws Exception {
        logger.info("TODO: Add test to test that commit messages are same, but the content is slightly different on the location of the patch");
    }

    @Test
    public void testSuspiciousCommits() throws Exception {
        logger.info("TODO: Add test to test that commit messages are same, but the content is different, maybe conflicts");
    }

    private MissingCommit testGitHub(String projectId, String revA, String revB) throws IOException {
        String username = TestUtils.getTestUserName();
        String password = TestUtils.getTestPassword();
        if (username == null || password == null) {
            logger.warn("No test gets executing, because no username nor password specified");
            return null;
        }
        GitRevMissing grm = GitRevMissing.create(new URL("https://github.com"), username, password);
        return grm.missingCommits(projectId, revA, revB);
    }

}
