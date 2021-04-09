package io.github.gaol.git_rev_missing;

final class RepoUtils {

    static String gitCommitLink(String repoURL, String sha) {
        if (repoURL.contains("github.com")) {
            return repoURL + "/commit/" + sha;
        } else if (repoURL.contains("gitlab")) {
            return repoURL + "~/commit/" + sha;
        }
        return repoURL + "/commit/" + sha;
    }

}
