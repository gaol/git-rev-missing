package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.repository.services.github.GitHubRepositoryService;
import org.jboss.set.aphrodite.repository.services.gitlab.GitLabRepositoryService;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.net.URI;
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

    private Map<String, RepositoryService> repositoryServiceMap = new ConcurrentHashMap<>();

    RepositoryService getRepositoryService(URI gitURI, String username, String password) {
        final String key = gitURI.getHost() + ":" + username;
        return repositoryServiceMap.computeIfAbsent(key, rs -> {
            RepositoryType repoType;
            RepositoryService repoService;
            if (gitURI.getHost().contains("github.com")) {
                repoType = RepositoryType.GITHUB;
                repoService = new GitHubRepositoryService();
            } else if (gitURI.getHost().contains("gitlab")) {
                repoType = RepositoryType.GITLAB;
                repoService = new GitLabRepositoryService();
            } else {
                //TODO support gitweb?
                throw new RuntimeException("Not supported for Git service: " + gitURI.toString());
            }
            RepositoryConfig repoConfig = new RepositoryConfig(gitURI.toString(), username, password, repoType);
            repoService.init(repoConfig);
            return repoService;
        });
    }

    void clear(String repoServiceKey) {
        RepositoryService service = repositoryServiceMap.remove(repoServiceKey);
        if (service != null) {
            service.destroy();
        }
    }

}
