package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.domain.Commit;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommitInfo)) return false;
        CommitInfo that = (CommitInfo) o;
        return commit.getSha().equals(that.commit.getSha()) &&
                Objects.equals(commitLink, that.commitLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commit, commitLink);
    }
}
