package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.github.gaol.git_rev_missing.RepoUtils.gitCommitLink;

class GitRevMissingImpl implements GitRevMissing {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private final RepositoryService repoService;
    private final String repoServiceKey;
    private final URL gitURL;
    private final URL gitRootURL;
    private boolean debug;

    GitRevMissingImpl(URL gitURL, String user, String pass) {
        super();
        Objects.requireNonNull(gitURL, "URI of the git service must be provided");
        gitRootURL = RepoUtils.canonicGitRootURL(gitURL);
        repoServiceKey = gitURL.getHost() + ":" + user;
        repoService = RepositoryServices.getInstance().getRepositoryService(gitRootURL, user, pass);
        this.gitURL = gitURL;
    }

    @Override
    public GitRevMissingImpl setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    @Override
    public MissingCommit missingCommits(String owner, String repo, String revA, String revB) {
        return missingCommits(owner, repo, revA, revB, Instant.now().toEpochMilli() - 6 * MONTH_MILLI);
    }

    @Override
    public MissingCommit missingCommits(String owner, String repo, String revA, String revB, long since) {
        try {
            URL repoURL = new URL(gitURL.toString() + "/" + owner + "/" + repo);
            List<Commit> revAList = repoService.getCommitsSince(repoURL, revA, since);
            List<Commit> revBList = repoService.getCommitsSince(repoURL, revB, since);
            if (revAList.isEmpty()) {
                System.out.println("# no commits found in revision: " + revA + " since: " + dateString(since));
            } else if (debug) {
                printList("RevA", revAList);
            }
            if (revBList.isEmpty()) {
                System.out.println("# no commits found in revision: " + revB + " since: " + dateString(since));
            } else if (debug) {
                printList("RevB", revBList);
            }
            List<CommitInfo> missingInB = new ArrayList<>();
            for (Commit commitInA: revAList) {
                if (!commitInList(commitInA, revBList)) {
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

    private void printList(String head, List<Commit> commits) {
        System.out.println("\n===========  [DEBUG INFO " + head + "]  ===========");
        for (Commit commit: commits) {
            System.out.println(commit);
        }
        System.out.println("===========  [DEBUG INFO END " + head + "]  ===========\n");
    }

    private static String dateString(long since) {
        Date date = new Date();
        date.setTime(since);
        return Instant.ofEpochMilli(since).toString();
    }

    private boolean commitInList(Commit commit, List<Commit> list) {
        for (Commit commitInList : list) {
            if (commit.getSha().equals(commitInList.getSha())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void release() {
        RepositoryServices.getInstance().clear(repoServiceKey);
    }

}
