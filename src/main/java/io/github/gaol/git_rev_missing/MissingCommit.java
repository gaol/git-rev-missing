package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.domain.Commit;

import java.util.List;

public class MissingCommit {

    private List<Commit> commits;

    public List<Commit> getCommits() {
        return commits;
    }

    public MissingCommit setCommits(List<Commit> commits) {
        this.commits = commits;
        return this;
    }

    public boolean isClean() {
        return commits == null || commits.isEmpty();
    }

    @Override
    public String toString() {
        return toString(null, false);
    }

    public String toString(String rootURI, boolean link) {
        StringBuilder sb = new StringBuilder();
        sb.append("MissingCommits:[\n");
        for (Commit commit: commits) {
            sb.append(commit.toString()).append("\n");
            if (link && rootURI != null) {
                sb.append("Link: ").append(rootURI + "/" + commit.getSha());
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
