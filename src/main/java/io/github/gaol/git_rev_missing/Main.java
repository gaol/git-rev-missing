package io.github.gaol.git_rev_missing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import org.jboss.set.aphrodite.repository.services.common.RepositoryUtils;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

@CommandLine.Command(name = "git_rev_missing", mixinStandardHelpOptions = true, version = "0.0.2",
        description = "Tool to list missing commits in a branch|tag compared to another one")
public class Main implements Callable<Integer> {

    private static final Log logger = LogFactory.getLog("g_r_m.main");

    @CommandLine.Parameters(index = "0", description = "The url of comparing the revisions, like: \nhttps://github.com/owner/repo/compare/1.0...1.1")
    private String compareURL;

    @CommandLine.Option(names = {"-u", "--user"}, description = "username used to interact with git service")
    private String username;

    @CommandLine.Option(names = {"-p", "--pass"}, description = "password used to interact with git service", interactive = true)
    private String password;

    @CommandLine.Option(names = {"-m", "--month"}, description = "how long to find commits, defaults to 1 year", defaultValue = "12", showDefaultValue = ALWAYS)
    private int month;

    @CommandLine.Option(paramLabel = "FILE", names = {"-c", "--config"}, description = "Config file, content is in JSON format.", defaultValue = "config.json", showDefaultValue = ALWAYS)
    private File configFile;

    @CommandLine.Option(names = {"-r", "--ratio"}, description = "Ratio threshold when compare the patches of 2 commits", defaultValue = "0.9d", showDefaultValue = ALWAYS)
    private double ratioThreshold;

    @CommandLine.Option(names = {"-s", "--message-ratio"}, description = "Ratio threshold when compare the messages of 2 suspicious commits", defaultValue = "0.7d", showDefaultValue = ALWAYS)
    private double messageRatioThreshold;

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
        final String gitRepoLink = compareURL.substring(0, lastSlash);
        URL gitRepoURL = new URL(gitRepoLink);
        if (versionLarger(revA, revB)) {
            // switch revA and revB
            String tmp = revA;
            revA = revB;
            revB = tmp;
        }
        if (revA.equals(revB)) {
            logger.info("Nothing to compare for the same version");
            return 0;
        }
        String[] repoID = RepositoryUtils.createRepositoryIdFromUrl(gitRepoURL).split("/");
        owner = repoID[0];
        repo = repoID[1];
        URL gitRootURL = RepoUtils.canonicGitRootURL(gitRepoURL);
        logger.debug("gitRoot: " + gitRootURL + ", owner: " + owner + ", repo: " + repo + ", revA: " + revA + ", revB: " + revB);
        if ((username == null || password == null) && configFile == null) {
            logger.error("No username/password nor config file specified.");
            return 1;
        }
        if (username == null || password == null) {
            if (!configFile.exists() && !configFile.getPath().startsWith("/")) {
                // try to check ~/config.json in home dir
                configFile = Paths.get(System.getProperty("user.home"), configFile.getName()).toFile();
            }
            logger.debug("Using Config File: " + configFile.getAbsolutePath());
            if (configFile.exists()) {
                try (JsonReader jr = Json.createReader(new FileInputStream(configFile))) {
                    JsonObject jsonObject = jr.readObject();
                    JsonArray configs = jsonObject.getJsonArray("repositoryConfigs");
                    if (configs == null) {
                        logger.error("No repositoryConfigs found in the config file");
                        return 1;
                    }
                    List<RepositoryConfig> repoConfigs = configs.stream()
                            .map(JsonObject.class::cast)
                            .map(json ->
                                    new RepositoryConfig(
                                            json.getString("url", null),
                                            json.getString("username", null),
                                            json.getString("password", null),
                                            RepositoryType.valueOf(json.getString("type", null))))
                            .collect(Collectors.toList());
                    repoConfigs.forEach(rc -> RepositoryServices.getInstance().add(rc));
                    RepositoryConfig config = RepoUtils.filterConfig(repoConfigs, gitRepoURL);
                    username = config.getUsername();
                    password = config.getPassword();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read the config file", e);
                }
            } else {
                logger.error("No a valid config file: " + configFile);
                return 1;
            }
        }
        GitRevMissing gitRevMissing = GitRevMissing.create(gitRootURL, username, password)
                .setRatioThreshold(ratioThreshold)
                .setMessageRatioThreshold(messageRatioThreshold)
                ;
        MissingCommit missCommit = gitRevMissing.missingCommits(owner, repo, revA, revB, Instant.now().toEpochMilli() - month * GitRevMissing.MONTH_MILLI);
        if (missCommit.isClean()) {
            logger.info("Great, no missing commits found\n");
        } else {
            if (missCommit.getCommits() != null && missCommit.getCommits().size() > 0) {
                logger.warn(missCommit.getCommits().size() + " commits were missing in " + revB + "\n");
            }
            if (missCommit.getSuspiciousCommits() != null && missCommit.getSuspiciousCommits().size() > 0) {
                logger.warn(missCommit.getSuspiciousCommits().size() + " commits were suspicious in " + revB + "\n");
            }
            logger.warn(missCommit.toString() + "\n");
        }
        gitRevMissing.release();
        return 0;
    }

    // return true if revA is larger than revB, false otherwise
    static boolean versionLarger(String revA, String revB) {
        String[] partsA = revA.split("\\.");
        String[] partsB = revB.split("\\.");
        for (int i=0, j=0; i < partsA.length || j < partsB.length;) {
            String partA = null, partB = null;
            if (i < partsA.length) {
                partA = partsA[i];
            }
            if (j < partsB.length) {
                partB = partsB[i];
            }
            if (partA != null && partB == null) {
                return true;
            }
            if (partA == null && partB != null) {
                return false;
            }
            int r = partCompare(partA, partB);
            if (r > 0) {
                return true;
            } else if (r < 0) {
                return false;
            }
            i++;
            j++;
        }
        return revA.compareTo(revB) > 0;
    }

    private static int partCompare(String p1, String p2) {
        // part of the version can be like: digit[0-9], SP[0-9], Final-redhat-0000X(), 1-SNAPSHOT, etc
        try {
            int n1 = Integer.parseInt(p1);
            int n2 = Integer.parseInt(p2);
            return n1 - n2;
        } catch (NumberFormatException e) {
            // not a number
        }
        return p1.compareTo(p2);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}
