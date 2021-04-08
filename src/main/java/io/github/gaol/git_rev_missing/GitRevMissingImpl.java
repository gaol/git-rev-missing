package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.repository.services.github.GitHubRepositoryService;
import org.jboss.set.aphrodite.repository.services.gitlab.GitLabRepositoryService;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

class GitRevMissingImpl implements GitRevMissing {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private final RepositoryService repoService;
    private final URI gitURI;

    private enum SupportedType {
        GITHUB,
        GITLAB
    }

    private final SupportedType type;

    private boolean debug;

    GitRevMissingImpl(URI gitURI, String user, String pass) {
        super();
        Objects.requireNonNull(gitURI, "URI of the git service must be provided");
        RepositoryType repoType;
        if (gitURI.getHost().contains("github.com")) {
            repoType = RepositoryType.GITHUB;
            repoService = new GitHubRepositoryService();
            type = SupportedType.GITHUB;
        } else if (gitURI.getHost().contains("gitlab")) {
            repoType = RepositoryType.GITLAB;
            repoService = new GitLabRepositoryService();
            type = SupportedType.GITLAB;
        } else {
            throw new RuntimeException("Not supported for Git service: " + gitURI.toString());
        }
        RepositoryConfig repoConfig = new RepositoryConfig(gitURI.toString(), user, pass, repoType);
        repoService.init(repoConfig);
        this.gitURI = gitURI;
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
            URL repoURL = new URL(gitURI.toString() + "/" + owner + "/" + repo);
            List<Commit> revAList = repoService.getCommitsSince(repoURL, revA, since);
            List<Commit> revBList = repoService.getCommitsSince(repoURL, revB, since);
            if (revAList.isEmpty()) {
                System.out.println("# no commits found in: " + revA + " since: " + dateString(since));
            } else if (debug) {
                printList("RevA", revAList);
            }
            if (revBList.isEmpty()) {
                System.out.println("# no commits found in: " + revB + " since: " + dateString(since));
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

    private String gitCommitLink(String repoURL, String sha) {
        if (type == SupportedType.GITHUB) {
            return repoURL + "/commit/" + sha;
        } else if (type == SupportedType.GITLAB) {
            return repoURL + "~/commit/" + sha;
        }
        return repoURL + "/commit/" + sha;
    }

    private static String dateString(long since) {
        Date date = new Date();
        date.setTime(since);
        return formatter.format(date);
    }

    private boolean commitInList(Commit commit, List<Commit> list) {
        for (Commit commitInList : list) {
            if (commit.getSha().equals(commitInList.getSha())) {
                return true;
            }
        }
        return false;
    }

}
