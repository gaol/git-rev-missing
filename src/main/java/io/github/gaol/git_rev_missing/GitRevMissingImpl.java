package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.domain.Commit;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.gaol.git_rev_missing.RepoUtils.gitCommitLink;

class GitRevMissingImpl implements GitRevMissing {

    private static final Logger logger = Logger.getLogger("g_r_m.impl");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final long MONTH_MILLI = 2629800000L;

    private final RepoService repoService;
    private final URL gitRootURL;
    private double ratioThreshold = 0.9d;
    private double messageRatioThreshold = 0.7d;
    private final String cacheKey;

    private static final ConcurrentHashMap<String, RepoService> repoServices = new ConcurrentHashMap<>();

    GitRevMissingImpl(URL gitRootURL, String user, String pass) {
        super();
        Objects.requireNonNull(gitRootURL, "URL of the git service root must be provided");
        Objects.requireNonNull(gitRootURL.getHost(), "Host of the git service root must be provided");
        this.gitRootURL = gitRootURL;
        cacheKey = user + "@" + gitRootURL.getHost();
        repoService = repoServices.computeIfAbsent(cacheKey, k -> RepoService.createRepoService(this.gitRootURL, user, pass));
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
    public MissingCommit missingCommits(String projectId, String revA, String revB) {
        return missingCommits(projectId, revA, revB, Instant.now().toEpochMilli() - 12 * MONTH_MILLI);
    }

    @Override
    public MissingCommit missingCommits(String projectId, String revA, String revB, long since) {
        try {
            URL repoURL = new URL(gitRootURL.toString() + "/" + projectId);
            logger.info("Checking commits between " + revA + " and " + revB + " in repository: " + repoURL);
            List<Commit> revAList = repoService.getCommitsSince(repoURL, revA, since);
            List<Commit> revBList = repoService.getCommitsSince(repoURL, revB, since);
            String sinceStr = dateString(since);
            if (revAList.isEmpty()) {
                logger.log(Level.WARNING, "# no commits found in revision: " + revA + " since: " + sinceStr + ", Please check if the revision: " + revA + " exists in " + projectId);
            } else {
                logger.info(revAList.size() + " commits are found in revision: " + revA + " since: " + sinceStr);
            }
            if (revBList.isEmpty()) {
                logger.log(Level.WARNING, "# no commits found in revision: " + revB + " since: " + sinceStr + ", Please check if the revision: " + revB + " exists in " + projectId);
            } else {
                logger.info(revBList.size() + " commits are found in revision: " + revB + " since: " + sinceStr);
            }
            List<CommitInfo> missingInB = new ArrayList<>();
            List<CommitInfo> suspiciousCommits = new ArrayList<>();
            for (Commit commitInA: revAList) {
                if (!shouldOmit(commitInA)) {
                    CompareResult result = commitInList(projectId, commitInA, revBList);
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
            throw new RuntimeException(e);
        }
    }

    private boolean shouldOmit(Commit commit) {
        String message = commit.getMessage();
        return message.startsWith("Merge branch ") || message.startsWith("Next is ")
                || message.startsWith("Prepare ") || message.startsWith("Merge pull request ")
                || message.equals("Repour");
    }

    private static String dateString(long since) {
        LocalDate date = Instant.ofEpochMilli(since)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return date.format(DATE_TIME_FORMATTER);
    }

    private CompareResult commitInList(String repoID, Commit commit, List<Commit> list) {
        CompareResult cr = new CompareResult().setSha1(commit.getSha());
        for (Commit commitInList : list) {
            if (commit.getSha().equals(commitInList.getSha())) {
                return cr.setResult(CompareResult.Result.SAME);
            }
        }
        List<Commit> sameMessageCommits = sameMessageInList(commit, list);
        if (!sameMessageCommits.isEmpty()) {
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
        List<Commit> similarMessage = similarMessageInList(commit, list, messageRatioThreshold);
        if (!similarMessage.isEmpty()) {
            boolean suspicious = false;
            for (Commit c: similarMessage) {
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
    public void close() {
        try {
            this.repoService.destroy();
        } finally {
            repoServices.remove(cacheKey);
        }
    }

}
