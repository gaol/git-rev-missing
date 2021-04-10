package io.github.gaol.git_rev_missing;

import org.gitlab4j.api.utils.JacksonJson;

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
        return JacksonJson.toJsonString(this);
    }

}
