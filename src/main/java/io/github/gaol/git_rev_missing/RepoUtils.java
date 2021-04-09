package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

final class RepoUtils {

    static RepositoryConfig filterConfig(List<RepositoryConfig> configs, URL url) {
        for (RepositoryConfig config: configs) {
            if (url.toString().startsWith(config.getUrl())) {
                return config;
            }
        }
        return null;
    }

    static String repoKey(String host, String username) {
        return host + ":" + username;
    }

    static String repoKey(RepositoryConfig config) {
        return repoKey(canonicRepoURL(config.getUrl()).getHost(), config.getUsername());
    }

    static URL canonicRepoURL(final String repoURL) {
        try {
            URL url = new URL(repoURL);
            String repoID = RepositoryUtils.createRepositoryIdFromUrl(url);
            return new URL(repoURL.substring(0, repoURL.indexOf(url.getPath())) + "/" + repoID);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    static String gitCommitLink(String repoURL, String sha) {
        try {
            URL repo = canonicRepoURL(repoURL);
            if (repo.toString().contains("gitlab")) {
                return repo + "/~/commit/" + sha;
            }
            return repo.toString() + "/commit/" + sha;
        } catch (Exception e) {
            return "<Not Known>";
        }
    }

}
