package io.github.gaol.git_rev_missing;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jboss.set.aphrodite.domain.Commit;

import java.util.Objects;

/**
 * CommitInfo represents the commit and the link to the commit.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class CommitInfo {

    /**
     * The commit information including sha and message
     */
    private Commit commit;

    /**
     * The commit link, it is the one of lower revision in the different commits list.
     */
    private String commitLink;

    /**
     * The target link, it is the one of higher revision in the suspicious commits list.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String targetLink;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getTargetLink() {
        return targetLink;
    }

    public CommitInfo setTargetLink(String targetLink) {
        this.targetLink = targetLink;
        return this;
    }

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
