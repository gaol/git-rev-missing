package io.github.gaol.git_rev_missing;

import java.net.URI;

public interface GitRevMissing {

    // milliseconds in a month
    long MONTH_MILLI = 2629800000L;

    static GitRevMissing create(URI gitURI, String user, String pass) {
        return new GitRevMissingImpl(gitURI, user, pass);
    }

    GitRevMissing setDebug(boolean debug);

    MissingCommit missingCommits(String owner, String repo, String revA, String revB);

    MissingCommit missingCommits(String owner, String repo, String revA, String revB, long since);

}
