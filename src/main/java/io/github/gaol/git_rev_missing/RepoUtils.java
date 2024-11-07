package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryUtils;
import org.jboss.set.aphrodite.repository.services.gitlab.GitLabUtils;

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

    static String projectId(URL gitRepoURL) {
        return GitLabUtils.getProjectIdFromURL(gitRepoURL);
    }

    // return the git root url, like: https://github.com
    static URL canonicGitRootURL(final URL gitURL) {
        RepositoryUtils.createRepositoryIdFromUrl(gitURL);
        if (gitURL.getPath() == null || gitURL.getPath().isEmpty()) {
            return gitURL;
        }
        try {
            String repoURL = gitURL.toString();
            String rootURL = repoURL.substring(0, repoURL.indexOf(gitURL.getPath()));
            return new URL(rootURL);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    // return the git repository url, like: https://github.com/ihomeland/prtest
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
            if (repo != null) {
                return repo.toString() + "/commit/" + sha;
            }
            return "<Not Known>";
        } catch (Exception e) {
            return "<Not Known>";
        }
    }

}
