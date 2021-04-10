package io.github.gaol.git_rev_missing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.domain.Commit;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.github.gaol.git_rev_missing.RepoUtils.gitCommitLink;

class GitRevMissingImpl implements GitRevMissing {

    private static final Log logger = LogFactory.getLog("git_rev_missing.impl");

    private final RepoService repoService;
    private final String repoServiceKey;
    private final URL gitRootURL;
    private final String username;
    private final String password;

    GitRevMissingImpl(URL gitRootURL, String user, String pass) {
        super();
        Objects.requireNonNull(gitRootURL, "URL of the git service root must be provided");
        this.gitRootURL = gitRootURL;
        repoServiceKey = this.gitRootURL.getHost() + ":" + user;
        repoService = RepositoryServices.getInstance().getRepositoryService(this.gitRootURL, user, pass);
        this.username = user;
        this.password = pass;
    }

    @Override
    public MissingCommit missingCommits(String owner, String repo, String revA, String revB) {
        return missingCommits(owner, repo, revA, revB, Instant.now().toEpochMilli() - 12 * MONTH_MILLI);
    }

    @Override
    public MissingCommit missingCommits(String owner, String repo, String revA, String revB, long since) {
        try {
            URL repoURL = new URL(gitRootURL.toString() + "/" + owner + "/" + repo);
            List<Commit> revAList = repoService.getCommitsSince(repoURL, revA, since);
            List<Commit> revBList = repoService.getCommitsSince(repoURL, revB, since);
            if (revAList.isEmpty()) {
                logger.warn("# no commits found in revision: " + revA + " since: " + dateString(since));
            } else {
                logger.info(revAList.size() + " commits are found in revision: " + revA + " since: " + dateString(since));
            }
            if (revBList.isEmpty()) {
                logger.warn("# no commits found in revision: " + revB + " since: " + dateString(since));
            } else {
                logger.info(revBList.size() + " commits are found in revision: " + revB + " since: " + dateString(since));
            }
            List<CommitInfo> missingInB = new ArrayList<>();
            for (Commit commitInA: revAList) {
                if (!shouldOmit(commitInA) && !commitInList(owner + "/" + repo, commitInA, revBList)) {
                    CommitInfo commitInfo = new CommitInfo();
                    commitInfo.setCommit(commitInA);
                    commitInfo.setCommitLink(gitCommitLink(repoURL.toString(), commitInA.getSha()));
                    missingInB.add(commitInfo);
                }
            }
            MissingCommit missingCommit = new MissingCommit();
            missingCommit.setCommits(missingInB);
            return missingCommit;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean shouldOmit(Commit commit) {
        String message = commit.getMessage();
        if (message.startsWith("Merge branch ") || message.startsWith("Next is ")
                || message.startsWith("Prepare ") || message.startsWith("Merge pull request ")) {
            return true;
        }
        return false;
    }

    private static String dateString(long since) {
        Date date = new Date();
        date.setTime(since);
        return Instant.ofEpochMilli(since).toString();
    }

    private boolean commitInList(String repoID, Commit commit, List<Commit> list) {
        for (Commit commitInList : list) {
            if (commit.getSha().equals(commitInList.getSha())) {
                return true;
            }
        }
        List<Commit> sameMessageCommits = sameMessageInList(commit, list);
        if (sameMessageCommits.size() > 0) {
            for (Commit c: sameMessageCommits) {
                if (repoService.commitSame(repoID, commit.getSha(), c.getSha())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Commit> sameMessageInList(Commit commit, List<Commit> list) {
        List<Commit> commits = new ArrayList<>();
        for (Commit c: list) {
            if (c.getMessage().equals(commit.getMessage())) {
                commits.add(c);
            }
        }
        return commits;
    }

    @Override
    public void release() {
        RepositoryServices.getInstance().clear(repoServiceKey);
    }

}
