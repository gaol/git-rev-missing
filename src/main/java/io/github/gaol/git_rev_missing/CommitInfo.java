package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.domain.Commit;

public class CommitInfo {
    private Commit commit;
    private String commitLink;

    public Commit getCommit() {
        return commit;
    }

    public CommitInfo setCommit(Commit commit) {
        this.commit = commit;
        return this;
    }

    public String getCommitLink() {
        return commitLink;
    }

    public CommitInfo setCommitLink(String commitLink) {
        this.commitLink = commitLink;
        return this;
    }

    @Override
    public String toString() {
        return "CommitInfo{" +
                "commit=" + commit +
                ", commitLink=" + commitLink +
                '}';
    }
}
