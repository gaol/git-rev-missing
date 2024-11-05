package io.github.gaol.git_rev_missing;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.domain.Commit;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.repository.services.github.GitHubRepositoryService;
import org.jboss.set.aphrodite.repository.services.gitlab.GitLabRepositoryService;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

abstract class RepoService {

    static RepoService createRepoService(URL gitRootURL, String username, String password) {
        RepoService repoService;
        if (gitRootURL.getHost().toLowerCase().contains("github.com")) {
            RepositoryConfig config = new RepositoryConfig(gitRootURL.toString(), username, password, RepositoryType.GITHUB);
            RepositoryService repositoryService = new GitHubRepositoryService();
            repositoryService.init(config);
            repoService = new GitHubRepoService(repositoryService);
        } else if (gitRootURL.getHost().toLowerCase().contains("gitlab")) {
            RepositoryConfig config = new RepositoryConfig(gitRootURL.toString(), username, password, RepositoryType.GITLAB);
            RepositoryService repositoryService = new GitLabRepositoryService();
            repositoryService.init(config);
            repoService = new GitLabRepoService(repositoryService);
        } else {
            throw new RuntimeException("Not supported for Git service: " + gitRootURL);
        }
        return repoService;
    }

    private final RepositoryService repositoryService;

    protected RepoService(RepositoryService repoService) {
        this.repositoryService = repoService;
    }

    List<Commit> getCommitsSince(URL repoURL, String branch, long since) {
        return repositoryService.getCommitsSince(repoURL, branch, since);
    }

    abstract CompareResult.Result commitSame(String repoIdOrName, String sha1, String sha2, double ratioThreshold);

    void destroy() {
        this.repositoryService.destroy();
    }

    static double similarness(String patch1, String patch2) {
        return new JaroWinklerDistance().apply(patch1, patch2);
    }

    static String commitMessageTrim(String message) {
        return message.replaceAll("\\[[^\n]+\\-[0-9]+\\]", "").trim();
    }

    static String trimPatchLocation(String patch) {
        return patch.replaceAll("@@ [^\n]+ @@", "");
    }

    protected  <T> T getInstanceField(Object obj, String fieldName) {
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
}
