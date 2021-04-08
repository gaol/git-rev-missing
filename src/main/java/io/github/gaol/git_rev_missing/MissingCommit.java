package io.github.gaol.git_rev_missing;

import java.util.List;

public class MissingCommit {

    private List<Commit> commits;

    private static class Commit {
        private String sha1;
        private String message;
    }

}
