package io.github.gaol.git_rev_missing;

class CompareResult {
    enum Result {
        // commit SHA1 is the same
        SAME,
        // both SHA1 and patch content are different
        DIFFERENT,
        // commit message is the same, but the patch has little difference, like 90% similar.
        SUSPICIOUS
    }

    private Result result;
    private String sha1;
    private String sha2;

    public Result getResult() {
        return result;
    }

    public CompareResult setResult(Result result) {
        this.result = result;
        return this;
    }

    public String getSha1() {
        return sha1;
    }

    public CompareResult setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public String getSha2() {
        return sha2;
    }

    public CompareResult setSha2(String sha2) {
        this.sha2 = sha2;
        return this;
    }
}
