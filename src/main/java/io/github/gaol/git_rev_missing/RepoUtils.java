package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.repository.services.common.RepositoryUtils;

import java.net.MalformedURLException;
import java.net.URL;

final class RepoUtils {

    static URL canonicRepoURL(final String repoURL) throws MalformedURLException {
        URL url = new URL(repoURL);
        String repoID = RepositoryUtils.createRepositoryIdFromUrl(url);
        return new URL(repoURL.substring(0, repoURL.indexOf(url.getPath())) + "/" + repoID);
    }

    static String gitCommitLink(String repoURL, String sha) {
        try {
            URL repo = canonicRepoURL(repoURL);
            if (repo.toString().contains("gitlab")) {
                return repo + "/~/commit/" + sha;
            }
            return repo.toString() + "/commit/" + sha;
        } catch (MalformedURLException e) {
            return "<Not Known>";
        }
    }

}
