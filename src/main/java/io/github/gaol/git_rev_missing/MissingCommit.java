package io.github.gaol.git_rev_missing;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.gitlab4j.api.utils.JacksonJson;

import java.util.List;

public class MissingCommit {

    private List<CommitInfo> commits;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CommitInfo> suspiciousCommits;

    public List<CommitInfo> getSuspiciousCommits() {
        return suspiciousCommits;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public MissingCommit setSuspiciousCommits(List<CommitInfo> suspiciousCommits) {
        this.suspiciousCommits = suspiciousCommits;
        return this;
    }

    public List<CommitInfo> getCommits() {
        return commits;
    }

    public MissingCommit setCommits(List<CommitInfo> commits) {
        this.commits = commits;
        return this;
    }

    public boolean isClean() {
        return (commits == null || commits.isEmpty()) && (suspiciousCommits == null || suspiciousCommits.isEmpty());
    }

    @Override
    public String toString() {
        return JacksonJson.toJsonString(this);
    }

}
