package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.config.RepositoryConfig;
import org.jboss.set.aphrodite.repository.services.common.RepositoryType;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

@CommandLine.Command(name = "git_rev_missing", mixinStandardHelpOptions = true, version = "0.0.2",
        description = "Tool to list missing commits in a branch|tag compared to another one")
public class Main implements Callable<Integer> {

    static {
        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (inputStream == null) {
                System.err.println("Could not find logging.properties inside the JAR.");
            } else {
                LogManager.getLogManager().readConfiguration(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Error loading logging configuration.");
        }
    }

    private static final Logger logger = Logger.getLogger("g_r_m.main");

    @CommandLine.Option(names = {"-r", "--repo"}, description = "The repository URL, like: https://github.com/owner/repo", required = true, order = 1)
    private String repoURL;

    @CommandLine.Option(names = {"-a", "--r1"}, description = "The lower revision as the base", required = true, order = 2)
    private String r1;

    @CommandLine.Option(names = {"-b", "--r2"}, description = "The higher revision as the target", required = true, order = 3)
    private String r2;

    @CommandLine.Option(names = {"-u", "--user"}, description = "username used to interact with git service")
    private String username;

    @CommandLine.Option(names = {"-p", "--pass"}, description = "password used to interact with git service", interactive = true)
    private String password;

    @CommandLine.Option(names = {"-m", "--month"}, description = "how long to find commits, defaults to 1 year", defaultValue = "12", showDefaultValue = ALWAYS)
    private int month;

    @CommandLine.Option(paramLabel = "FILE", names = {"-c", "--config"}, description = "Config file, content is in JSON format. See example from ./config.json.example", defaultValue = "~/config.json", showDefaultValue = ALWAYS)
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
        logger.log(Level.FINE, "gitRoot: " + gitRootURL + ", projectId: " + projectId + ", r1: " + r1 + ", r2: " + r2);
        if ((username == null || password == null) && configFile == null) {
            logger.log(Level.SEVERE, "No username/password nor config file specified.");
            return 1;
        }
        if (username == null || password == null) {
            if (!configFile.exists() && !configFile.isAbsolute()) {
                // try to check ~/config.json in home dir
                configFile = Paths.get(System.getProperty("user.home"), configFile.getName()).toFile();
            }
            logger.log(Level.FINE, "Using Config File: " + configFile.getAbsolutePath());
            if (configFile.exists()) {
                try (JsonReader jr = Json.createReader(Files.newInputStream(configFile.toPath()))) {
                    JsonObject jsonObject = jr.readObject();
                    JsonArray configs = jsonObject.getJsonArray("repositoryConfigs");
                    if (configs == null) {
                        logger.log(Level.SEVERE, "No repositoryConfigs found in the config file");
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
                logger.log(Level.SEVERE, "No a valid config file: " + configFile);
                return 1;
            }
        }
        try (GitRevMissing gitRevMissing = GitRevMissing.create(gitRootURL, username, password)) {
            MissingCommit missCommit = gitRevMissing.missingCommits(projectId, r1, r2, Instant.now().toEpochMilli() - month * GitRevMissingImpl.MONTH_MILLI);
            if (missCommit.isClean()) {
                logger.info("Great, no missing commits found\n");
            } else {
                if (missCommit.getCommits() != null && !missCommit.getCommits().isEmpty()) {
                    logger.log(Level.WARNING, missCommit.getCommits().size() + " commits were missing in " + r2 + "\n");
                }
                if (missCommit.getSuspiciousCommits() != null && !missCommit.getSuspiciousCommits().isEmpty()) {
                    logger.log(Level.WARNING, missCommit.getSuspiciousCommits().size() + " commits were suspicious in " + r1 + "\n");
                }
                logger.log(Level.WARNING, missCommit + "\n");
            }
        }
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}
