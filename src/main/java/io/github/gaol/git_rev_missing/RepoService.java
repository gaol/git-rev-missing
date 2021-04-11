package io.github.gaol.git_rev_missing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.similarity.JaroWinklerDistance;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RepoService {

    private static final Log logger = LogFactory.getLog("g_r_m.repo.service");

    // this is hack before the methods are integreated into Aphrodite
    private static final String GITHUB_API_PAMA_NAME = "github";
    private static final String GITLAB_API_PAMA_NAME = "gitLabApi";

    private RepositoryService repoService;
    private GitLabApi gitLabApi;
    private GitHub github;

    private final Map<String, List<GHCommit.File>> githubCachedFiles = new ConcurrentHashMap<>();
    private final Map<String, List<Diff>> gitlabCachedFiles = new ConcurrentHashMap<>();

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

    private List<GHCommit.File> getGitHubCommitFiles(String repoId, String sha) {
        try {
            GHRepository repository = github.getRepository(repoId);
            if (repository == null) {
                throw new RuntimeException("No repository: " + repoId + " was found");
            }
            GHCommit commit = repository.getCommit(sha);
            if (commit == null) {
                throw new RuntimeException("No commit: " + sha + " was found in " + repoId);
            }
            return commit.getFiles();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compare 2 commits", e);
        }
    }

    private CompareResult.Result commitSameGitHub(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        List<GHCommit.File> files1 = githubCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha1, v -> getGitHubCommitFiles(repoIdOrName, sha1));
        List<GHCommit.File> files2 = githubCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha2, v -> getGitHubCommitFiles(repoIdOrName, sha2));
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
        return CompareResult.Result.SAME;
    }

    static double similarness(String patch1, String patch2) {
        return new JaroWinklerDistance().apply(patch1, patch2);
    }

    static String commitMessageTrim(String message) {
        return message.replaceAll("\\[[^\n]+\\-[0-9]+\\]", "").trim();
    }

    private CompareResult.Result commitSameGitLab(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        List<Diff> diffs1 = gitlabCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha1, v -> {
            try {
                return gitLabApi.getCommitsApi().getDiff(repoIdOrName, sha1);
            } catch (GitLabApiException e) {
                throw new RuntimeException("Failed to get differnece of commit: " + sha1, e);
            }
        });
        List<Diff> diffs2 = gitlabCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha2, v -> {
            try {
                return gitLabApi.getCommitsApi().getDiff(repoIdOrName, sha2);
            } catch (GitLabApiException e) {
                throw new RuntimeException("Failed to get differnece of commit: " + sha2, e);
            }
        });
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
                if (similar > ratioThreshold) {
                    return CompareResult.Result.SUSPICIOUS;
                }
                return CompareResult.Result.DIFFERENT;
            }
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
        githubCachedFiles.clear();
        gitlabCachedFiles.clear();
    }

}
