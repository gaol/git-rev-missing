package io.github.gaol.git_rev_missing;

import java.net.URL;

/**
 * This provides a simple API to find missing commits when upgrading from one version to another.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public interface GitRevMissing extends AutoCloseable {

    /**
     * milliseconds in a month
      */
    long MONTH_MILLI = 2629800000L;

    /**
     * Creates the <code>GitRevMissing</code> instance
     *
     * @param gitRootURL the git service URL, it must be the root url of the git service.
     * @param user the username to communicate with the git service
     * @param pass the password of the username
     * @return a new <code>GitRevMissing</code> instance
     */
    static GitRevMissing create(URL gitRootURL, String user, String pass) {
        return new GitRevMissingImpl(gitRootURL, user, pass);
    }

    /**
     * Sets the ratio threshold when comparing the patches, default to <code>0.9d</code>
     *
     * @param ratioThreshold the ratio threshold of the patch difference
     * @return this reference for confluent use
     */
    GitRevMissing setRatioThreshold(double ratioThreshold);

    /**
     * Sets the ratio threshold when comparing the commit messages for suspicious commits, default to <code>0.7d</code>
     *
     * @param messageRatioThreshold the ratio threshold of the commit message difference
     * @return this reference for confluent use
     */
    GitRevMissing setMessageRatioThreshold(double messageRatioThreshold);

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
    void close();
}
