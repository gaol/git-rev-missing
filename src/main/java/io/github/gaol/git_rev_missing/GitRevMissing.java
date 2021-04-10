package io.github.gaol.git_rev_missing;

import java.net.URL;

/**
 * This provides a simple API to find missing commits when upgrading from one version to another.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public interface GitRevMissing {

    /**
     * milliseconds in a month
      */
    long MONTH_MILLI = 2629800000L;

    /**
     * Creates the <code>GitRevMissing</code> instance
     *
     * @param gitURL the git service URL, it
     * @param user the username to communicate with the git service
     * @param pass the password of the username
     * @return a new <code>GitRevMissing</code> instance
     */
    static GitRevMissing create(URL gitURL, String user, String pass) {
        return new GitRevMissingImpl(gitURL, user, pass);
    }

    /**
     * Tries to find commits in <code>revA</code>, but missing in <code>revB</code>.
     * <p>
     *     It tried to find commits from 12 months ago.
     * </p>
     * @param owner the owner of the repository
     * @param repo the repository name
     * @param revA revision A from which the commits are listed.
     * @param revB revision B to which the the commits may be missing.
     * @return a MissingCommit represents the result.
     */
    MissingCommit missingCommits(String owner, String repo, String revA, String revB);

    /**
     * Tries to find commits in <code>revA</code>, but missing in <code>revB</code>.
     * <p>
     *     It tried to find commits from 12 months ago.
     * </p>
     * @param owner the owner of the repository
     * @param repo the repository name
     * @param revA revision A from which the commits are listed.
     * @param revB revision B to which the the commits may be missing.
     * @param since time in milliseconds from when to find commits
     * @return a MissingCommit represents the result.
     */
    MissingCommit missingCommits(String owner, String repo, String revA, String revB, long since);

    /**
     * Release the resources
     */
    void release();
}
