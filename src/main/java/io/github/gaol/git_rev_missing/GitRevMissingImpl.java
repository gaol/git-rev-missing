package io.github.gaol.git_rev_missing;

import java.net.URI;
import java.time.Instant;

class GitRevMissingImpl implements GitRevMissing {

    // milliseconds in a month
    private static final long MONTH_MILLI = 2629800000L;

    GitRevMissingImpl(URI gitURI, String owner, String repo) {
        super();
    }

    @Override
    public MissingCommit missingCommits(String revA, String revB) {
        return missingCommits(revA, revB, Instant.now().toEpochMilli() - 6 * MONTH_MILLI);
    }

    @Override
    public MissingCommit missingCommits(String revA, String revB, long since) {
        return null;
    }

}
