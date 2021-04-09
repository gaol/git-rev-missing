package io.github.gaol.git_rev_missing;

import picocli.CommandLine;

import java.net.URI;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "git-rev-missing", mixinStandardHelpOptions = true, version = "0.0.1",
        description = "Tool to list missing commits in a branch|tag compared to another one")
public class Main implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The url of comparing the revisions")
    private String compareURL;

    @CommandLine.Option(names = {"-u", "--user"}, description = "username used to interact with git service")
    private String username;

    @CommandLine.Option(names = {"-p", "--pass"}, description = "password used to interact with git service")
    private String password;

    @CommandLine.Option(names = {"-m", "--month"}, description = "time in month to check the commits, defaults to 6 months", defaultValue = "6")
    private int month;

    @CommandLine.Option(names = {"-d", "--debug"}, description = "debug for verbose info", defaultValue = "false")
    private boolean debug;

    @Override
    public Integer call() throws Exception {
        // for gitlab, like: https://gitlab.xxx.com/owner/repo/-/compare/revB...revA
        // for github, like: https://github.com/ihomeland/prtest/compare/1.0.0...1.0.2
        String owner, repo, revA, revB;
        int dotsIdx = compareURL.indexOf("...");
        if (dotsIdx == -1) {
            //TODO for gitweb, it is different
            throw new RuntimeException("Cannot parse compareURL: " + compareURL);
        }
        revB = compareURL.substring(dotsIdx + 3);
        int lastSlash = compareURL.lastIndexOf("/");
        revA = compareURL.substring(lastSlash + 1, dotsIdx);
        URI leftURI = new URI(compareURL.substring(0, lastSlash));
        String path = leftURI.getPath();
        String hostPart = compareURL.substring(0, compareURL.indexOf(path));
        if (leftURI.getHost().contains("gitlab")) {
            // switch revA and revB
            String tmp = revA;
            revA = revB;
            revB = tmp;
        }
        URI gitRoot = new URI(hostPart);
        String[] parseplist = path.substring(1).split("/");
        owner = parseplist[0];
        repo = parseplist[1];
        if (debug) {
            System.out.println("gitRoot: " + gitRoot + ", owner: " + owner + ", repo: " + repo + ", revA: " + revA + ", revB: " + revB);
        }

        GitRevMissing gitRevMissing = GitRevMissing.create(gitRoot, username, password).setDebug(debug);
        MissingCommit missCommit = gitRevMissing.missingCommits(owner, repo, revA, revB, month * GitRevMissing.MONTH_MILLI);
        if (missCommit.isClean()) {
            System.out.println("Great, no missing commits found");
        } else {
            System.err.println("Missing Commits Found:");
            System.err.println(missCommit.toString());
        }
        System.out.println();
        gitRevMissing.release();
        return 0;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}
