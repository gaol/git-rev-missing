package io.github.gaol.git_rev_missing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

@CommandLine.Command(name = "git_rev_missing", mixinStandardHelpOptions = true, version = "0.0.2",
        description = "Tool to list missing commits in a branch|tag compared to another one")
public class Main implements Callable<Integer> {

    private static final Log logger = LogFactory.getLog("git_rev_missing.main");

    @CommandLine.Option(names = {"-r", "--repo"}, description = "The repository URL, like: https://github.com/owner/repo\"", required = true)
    private String repoURL;

    @CommandLine.Option(names = {"-a", "--r1"}, description = "The lower revision as the base", required = true)
    private String r1;

    @CommandLine.Option(names = {"-b", "--r2"}, description = "The higher revision as the target", required = true)
    private String r2;

    @CommandLine.Option(names = {"-u", "--user"}, description = "username used to interact with git service")
    private String username;

    @CommandLine.Option(names = {"-p", "--pass"}, description = "password used to interact with git service", interactive = true)
    private String password;

    @CommandLine.Option(names = {"-m", "--month"}, description = "how long to find commits, defaults to 1 year", defaultValue = "12", showDefaultValue = ALWAYS)
    private int month;

    @CommandLine.Option(paramLabel = "FILE", names = {"-c", "--config"}, description = "Config file, content is in JSON format.", defaultValue = "~/config.json", showDefaultValue = ALWAYS)
    private File configFile;

    @Override
    public Integer call() throws Exception {
        if (r1.equals(r2)) {
            logger.info("Nothing to compare for the same version");
            return 0;
        }
        URL gitRepoURL = new URL(repoURL);
        final String projectId = RepoUtils.projectId(gitRepoURL);
        logger.info("projectId: " + projectId);
        URL gitRootURL = RepoUtils.canonicGitRootURL(gitRepoURL);
        logger.debug("gitRoot: " + gitRootURL + ", projectId: " + projectId + ", r1: " + r1 + ", r2: " + r2);
        if ((username == null || password == null) && configFile == null) {
            logger.error("No username/password nor config file specified.");
            return 1;
        }
        if (username == null || password == null) {
            if (!configFile.exists() && !configFile.isAbsolute()) {
                // try to check ~/config.json in home dir
                configFile = Paths.get(System.getProperty("user.home"), configFile.getName()).toFile();
            }
            logger.debug("Using Config File: " + configFile.getAbsolutePath());
            if (configFile.exists()) {
                try (JsonReader jr = Json.createReader(Files.newInputStream(configFile.toPath()))) {
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
                    RepositoryConfig config = RepoUtils.filterConfig(repoConfigs, gitRepoURL);
                    if (config != null) {
                        username = config.getUsername();
                        password = config.getPassword();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read the config file", e);
                }
            } else {
                logger.error("No a valid config file: " + configFile);
                return 1;
            }
        }
        try (GitRevMissing gitRevMissing = GitRevMissing.create(gitRootURL, username, password)) {
            MissingCommit missCommit = gitRevMissing.missingCommits(projectId, r1, r2, Instant.now().toEpochMilli() - month * GitRevMissing.MONTH_MILLI);
            if (missCommit.isClean()) {
                logger.info("Great, no missing commits found\n");
            } else {
                if (missCommit.getCommits() != null && !missCommit.getCommits().isEmpty()) {
                    logger.warn(missCommit.getCommits().size() + " commits were missing in " + r2 + "\n");
                }
                if (missCommit.getSuspiciousCommits() != null && !missCommit.getSuspiciousCommits().isEmpty()) {
                    logger.warn(missCommit.getSuspiciousCommits().size() + " commits were suspicious in " + r1 + "\n");
                }
                logger.warn(missCommit + "\n");
            }
        }
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}
