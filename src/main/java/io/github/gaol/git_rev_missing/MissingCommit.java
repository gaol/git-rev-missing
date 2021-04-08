package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.domain.Commit;

import java.util.List;

public class MissingCommit {

    private List<CommitInfo> commits;

    public List<CommitInfo> getCommits() {
        return commits;
    }

    public MissingCommit setCommits(List<CommitInfo> commits) {
        this.commits = commits;
        return this;
    }

    public boolean isClean() {
        return commits == null || commits.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MissingCommits:[\n");
        for (CommitInfo commit: commits) {
            sb.append(commit.toString()).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

}
