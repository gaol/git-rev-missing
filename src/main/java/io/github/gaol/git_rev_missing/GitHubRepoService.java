/*
 *  Copyright (c) 2024 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.github.gaol.git_rev_missing;

import org.jboss.set.aphrodite.spi.RepositoryService;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class GitHubRepoService extends RepoService {

    private static final String GITHUB_API_PAMA_NAME = "github";
    private final Map<String, List<GHCommit.File>> githubCachedFiles = new ConcurrentHashMap<>();
    private final GitHub github;

    GitHubRepoService(RepositoryService repoService) {
        super(repoService);
        github = getInstanceField(repoService, GITHUB_API_PAMA_NAME);
    }

    @Override
    CompareResult.Result commitSame(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        List<GHCommit.File> files1 = githubCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha1, v -> getGitHubCommitFiles(repoIdOrName, sha1));
        List<GHCommit.File> files2 = githubCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha2, v -> getGitHubCommitFiles(repoIdOrName, sha2));
        if (files1.size() != files2.size()) {
            return CompareResult.Result.DIFFERENT;
        }
        for (int i = 0; i < files1.size(); i ++) {
            GHCommit.File f1 = files1.get(i);
            GHCommit.File f2 = files2.get(i);
            if (!f1.getPatch().equals(f2.getPatch())) {
                final String p1 = trimPatchLocation(f1.getPatch());
                final String p2 = trimPatchLocation(f2.getPatch());
                if (p1.equals(p2)) {
                    return CompareResult.Result.SAME;
                }
                // check if only small differences, like conflicts resolved, or different locations about the diff
                double similar = similarness(p1, p2);
                if (similar > ratioThreshold) {
                    return CompareResult.Result.SUSPICIOUS;
                }
                return CompareResult.Result.DIFFERENT;
            }
        }
        return CompareResult.Result.SAME;
    }

    private List<GHCommit.File> getGitHubCommitFiles(String repoId, String sha) {
        try {
            GHRepository repository = github.getRepository(repoId);
            if (repository == null) {
                throw new RuntimeException("No repository: " + repoId + " was found");
            }
            GHCommit commit = repository.getCommit(sha);
            if (commit == null) {
                throw new RuntimeException("No commit: " + sha + " was found in " + repoId);
            }
            return commit.getFiles();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compare 2 commits", e);
        }
    }

    @Override
    void destroy() {
        super.destroy();
        this.githubCachedFiles.clear();
    }
}
