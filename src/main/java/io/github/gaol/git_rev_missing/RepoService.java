package io.github.gaol.git_rev_missing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.gitlab4j.api.CommitsApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Diff;
import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.repository.services.github.GitHubRepositoryService;
import org.jboss.set.aphrodite.repository.services.gitlab.GitLabRepositoryService;
import org.jboss.set.aphrodite.spi.RepositoryService;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

class RepoService {

    private static final Log logger = LogFactory.getLog("g_r_m.repo.service");

    // this is hack before the methods are integreated into Aphrodite
    private static final String GITHUB_API_PAMA_NAME = "github";
    private static final String GITLAB_API_PAMA_NAME = "gitLabApi";

    private RepositoryService repoService;
    private GitLabApi gitLabApi;
    private GitHub github;

    public RepoService setRepoService(RepositoryService repoService) {
        this.repoService = repoService;
        if (repoService instanceof GitHubRepositoryService) {
            github = getInstanceField(repoService, GITHUB_API_PAMA_NAME);
        } else if (repoService instanceof GitLabRepositoryService) {
            gitLabApi = getInstanceField(repoService, GITLAB_API_PAMA_NAME);
        }
        return this;
    }

    private <T> T getInstanceField(Object obj, String fieldName) {
        try {
            Field field = null;
            Class<?> cls = obj.getClass();
            while (field == null && cls != null) {
                try {
                    field = cls.getDeclaredField(fieldName);
                } catch (NoSuchFieldException nfe) {
                    cls = cls.getSuperclass();
                }
            }
            if (field == null) {
                throw new RuntimeException("Not able to initialize the API from " + obj.getClass().getSimpleName());
            }
            field.setAccessible(true);
            return (T)field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Not able to initialize the API from " + obj.getClass().getSimpleName(), e);
        }
    }

    List<Commit> getCommitsSince(URL repoURL, String branch, long since) {
        return repoService.getCommitsSince(repoURL, branch, since);
    }

    CompareResult.Result commitSame(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        if (github != null) {
            return commitSameGitHub(repoIdOrName, sha1, sha2, ratioThreshold);
        } else if (gitLabApi != null) {
            return commitSameGitLab(repoIdOrName, sha1, sha2, ratioThreshold);
        } else {
            throw new RuntimeException("Not supported to get the diff of the commit");
        }
    }

    private CompareResult.Result commitSameGitHub(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        try {
            GHRepository repository = github.getRepository(repoIdOrName);
            if (repository == null) {
                throw new RuntimeException("No repository: " + repoIdOrName + " was found");
            }
            GHCommit commit1 = repository.getCommit(sha1);
            GHCommit commit2 = repository.getCommit(sha2);
            if (commit1 == null || commit2 == null) {
                return CompareResult.Result.DIFFERENT;
            }
            List<GHCommit.File> files1 = commit1.getFiles();
            List<GHCommit.File> files2 = commit2.getFiles();
            if (files1.size() != files2.size()) {
                return CompareResult.Result.DIFFERENT;
            }
            for (int i = 0; i < files1.size(); i ++) {
                GHCommit.File f1 = files1.get(i);
                GHCommit.File f2 = files2.get(i);
                if (!f1.getPatch().equals(f2.getPatch())) {
                    final String p1 = trimPatchLocation(f1.getPatch());
                    final String p2 = trimPatchLocation(f2.getPatch());
                    if (p1.equals(p2)) {
                        return CompareResult.Result.SAME;
                    }
                    // check if only small differences, like conflicts resolved, or different locations about the diff
                    double similar = similarness(p1, p2);
                    if (similar > ratioThreshold) {
                        return CompareResult.Result.SUSPICIOUS;
                    }
                    return CompareResult.Result.DIFFERENT;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to compare 2 commits", e);
        }
        return CompareResult.Result.SAME;
    }

    static double similarness(String patch1, String patch2) {
        return new JaroWinklerDistance().apply(patch1, patch2);
    }

    static String commitMessageTrim(String message) {
        return message.replaceAll("\\[[^\n]+\\-[0-9]+\\]", "").trim();
    }

    private CompareResult.Result commitSameGitLab(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        try {
            final CommitsApi commitsApi = gitLabApi.getCommitsApi();
            List<Diff> diffs1 = commitsApi.getDiff(repoIdOrName, sha1);
            List<Diff> diffs2 = commitsApi.getDiff(repoIdOrName, sha2);
            if (diffs1.size() != diffs2.size()) {
                return CompareResult.Result.DIFFERENT;
            }
            for (int i = 0; i < diffs1.size(); i ++) {
                Diff diff1 = diffs1.get(i);
                Diff diff2 = diffs2.get(i);
                if (!diff1.getDiff().equals(diff2.getDiff())) {
                    final String p1 = trimPatchLocation(diff1.getDiff());
                    final String p2 = trimPatchLocation(diff2.getDiff());
                    if (p1.equals(p2)) {
                        return CompareResult.Result.SAME;
                    }
                    // check if only small differences, like conflicts resolved, or different locations about the diff
                    double similar = similarness(p1, p2);
                    logger.debug("Similar between commit: " + sha1 + " and commit: " + sha2 + " is: " + similar);
                    if (similar > ratioThreshold) {
                        return CompareResult.Result.SUSPICIOUS;
                    }
                    return CompareResult.Result.DIFFERENT;
                }
            }
        } catch (GitLabApiException e) {
            throw new RuntimeException("Failed to compare 2 commits", e);
        }
        return CompareResult.Result.SAME;
    }

    static String trimPatchLocation(String patch) {
        return patch.replaceAll("@@ [^\n]+ @@", "");
    }

    public void destroy() {
        if (repoService != null) {
            repoService.destroy();
        }
    }

}
