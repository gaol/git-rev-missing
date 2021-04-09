package io.github.gaol.git_rev_missing;

import java.net.URL;

public interface GitRevMissing {

    // milliseconds in a month
    long MONTH_MILLI = 2592000000L;

    static GitRevMissing create(URL gitURL, String user, String pass) {
        return new GitRevMissingImpl(gitURL, user, pass);
    }

    GitRevMissing setDebug(boolean debug);

    MissingCommit missingCommits(String owner, String repo, String revA, String revB);

    MissingCommit missingCommits(String owner, String repo, String revA, String revB, long since);

    void release();
}
