package io.github.gaol.git_rev_missing;

import java.net.URI;

public interface GitRevMissing {

    static GitRevMissing create(URI gitURI, String user, String pass) {
        return new GitRevMissingImpl(gitURI, user, pass);
    }

    MissingCommit missingCommits(String owner, String repo, String revA, String revB);

    MissingCommit missingCommits(String owner, String repo, String revA, String revB, long since);

}
