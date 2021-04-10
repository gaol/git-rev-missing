package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.repository.services.github.GitHubRepositoryService;
import org.jboss.set.aphrodite.repository.services.gitlab.GitLabRepositoryService;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RepositoryServices {

    private static RepositoryServices instance = new RepositoryServices();
    private RepositoryServices() {
        // private
    }

    static RepositoryServices getInstance() {
        return instance;
    }

    private final Map<String, RepoService> repositoryServiceMap = new ConcurrentHashMap<>();
    private final Map<String, RepositoryConfig> configMap = new ConcurrentHashMap<>();

    RepoService getRepositoryService(URL gitRootURL, String username, String password) {
        final String key = gitRootURL.getHost() + ":" + username;
        final RepositoryConfig config = configMap.computeIfAbsent(key, c -> {
            RepositoryType repoType;
            if (gitRootURL.getHost().contains("github.com")) {
                repoType = RepositoryType.GITHUB;
            } else if (gitRootURL.getHost().contains("gitlab")) {
                repoType = RepositoryType.GITLAB;
            } else {
                //TODO support gitweb?
                throw new RuntimeException("Not supported for Git service: " + gitRootURL.toString());
            }
            return new RepositoryConfig(gitRootURL.toString(), username, password, repoType);
        });
        return repositoryServiceMap.computeIfAbsent(key, rs -> {
            RepositoryService repoService;
            if (RepositoryType.GITHUB.equals(config.getType())) {
                repoService = new GitHubRepositoryService();
            } else if (RepositoryType.GITLAB.equals(config.getType())) {
                repoService = new GitLabRepositoryService();
            } else {
                //TODO support gitweb?
                throw new RuntimeException("Not supported for Git service: " + gitRootURL.toString());
            }
            repoService.init(config);
            RepoService service = new RepoService();
            service.setRepoService(repoService);
            return service;
        });
    }

    void clear(String repoKey) {
        RepoService service = repositoryServiceMap.remove(repoKey);
        if (service != null) {
            service.destroy();
        }
        configMap.remove(repoKey);
    }

    public void add(RepositoryConfig rc) {
        configMap.putIfAbsent(RepoUtils.repoKey(rc), rc);
    }


}
