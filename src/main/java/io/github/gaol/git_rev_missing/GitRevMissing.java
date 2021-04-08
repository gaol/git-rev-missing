package io.github.gaol.git_rev_missing;

import java.net.URI;

public interface GitRevMissing {

    static GitRevMissing create(URI gitURI, String owner, String repo) {
        return new GitRevMissingImpl(gitURI, owner, repo);
    }

    MissingCommit missingCommits(String revA, String revB);

    MissingCommit missingCommits(String revA, String revB, long since);

}
