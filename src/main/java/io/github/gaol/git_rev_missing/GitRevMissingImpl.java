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

    private static final Log logger = LogFactory.getLog("g_r_m.impl");

    private final RepoService repoService;
    private final String repoServiceKey;
    private final URL gitRootURL;
    private final String username;
    private final String password;
    private double ratioThreshold = 0.9d;
    private double messageRatioThreshold = 0.7d;

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
    public GitRevMissingImpl setRatioThreshold(double ratioThreshold) {
        this.ratioThreshold = ratioThreshold;
        return this;
    }

    @Override
    public GitRevMissingImpl setMessageRatioThreshold(double messageRatioThreshold) {
        this.messageRatioThreshold = messageRatioThreshold;
        return this;
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
            List<CommitInfo> suspiciousCommits = new ArrayList<>();
            for (Commit commitInA: revAList) {
                if (!shouldOmit(commitInA)) {
                    CompareResult result = commitInList(owner + "/" + repo, commitInA, revBList);
                    if (result.getResult() == CompareResult.Result.DIFFERENT) {
                        CommitInfo commitInfo = new CommitInfo();
                        commitInfo.setCommit(commitInA);
                        commitInfo.setCommitLink(gitCommitLink(repoURL.toString(), commitInA.getSha()));
                        missingInB.add(commitInfo);
                    } else if (result.getResult() == CompareResult.Result.SUSPICIOUS) {
                        CommitInfo commitInfo = new CommitInfo();
                        commitInfo.setCommit(commitInA);
                        commitInfo.setCommitLink(gitCommitLink(repoURL.toString(), commitInA.getSha()));
                        commitInfo.setTargetLink(gitCommitLink(repoURL.toString(), result.getSha2()));
                        suspiciousCommits.add(commitInfo);
                    }
                }
            }
            MissingCommit missingCommit = new MissingCommit();
            missingCommit.setCommits(missingInB);
            missingCommit.setSuspiciousCommits(suspiciousCommits);
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

    private CompareResult commitInList(String repoID, Commit commit, List<Commit> list) {
        CompareResult cr = new CompareResult().setSha1(commit.getSha());
        for (Commit commitInList : list) {
            if (commit.getSha().equals(commitInList.getSha())) {
                return cr.setResult(CompareResult.Result.SAME);
            }
        }
        List<Commit> sameMessageCommits = sameMessageInList(commit, list);
        if (sameMessageCommits.size() > 0) {
            boolean suspicious = false;
            for (Commit c: sameMessageCommits) {
                CompareResult.Result result = repoService.commitSame(repoID, commit.getSha(), c.getSha(), ratioThreshold);
                if (CompareResult.Result.SAME == result) {
                    return cr.setResult(CompareResult.Result.SAME);
                } else if (result == CompareResult.Result.SUSPICIOUS) {
                    suspicious = true;
                    cr.setSha2(c.getSha());
                }
            }
            if (suspicious) {
                return cr.setResult(CompareResult.Result.SUSPICIOUS);
            }
        }
        // sometime, the commit message got amended, but the patch content is the same, we consider that as the same commit
        List<Commit> simiarMessage = similarMessageInList(commit, list, messageRatioThreshold);
        if (simiarMessage.size() > 0) {
            boolean suspicious = false;
            for (Commit c: simiarMessage) {
                CompareResult.Result result = repoService.commitSame(repoID, commit.getSha(), c.getSha(), ratioThreshold);
                if (CompareResult.Result.SAME == result) {
                    return cr.setResult(CompareResult.Result.SAME);
                } else if (result == CompareResult.Result.SUSPICIOUS) {
                    suspicious = true;
                    cr.setSha2(c.getSha());
                }
            }
            if (suspicious) {
                return cr.setResult(CompareResult.Result.SUSPICIOUS);
            }
        }
        return cr.setResult(CompareResult.Result.DIFFERENT);
    }

    private List<Commit> similarMessageInList(Commit commit, List<Commit> list, double messageRatioThreshold) {
        List<Commit> commits = new ArrayList<>();
        for (Commit c: list) {
            if (!c.getMessage().equals(commit.getMessage())) {
                if (RepoService.commitMessageTrim(c.getMessage()).equals(RepoService.commitMessageTrim(commit.getMessage()))
                || RepoService.similarness(c.getMessage(), commit.getMessage()) > messageRatioThreshold) {
                    // after trim the issue key, it is the same
                    commits.add(c);
                }
            }
        }
        return commits;
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
