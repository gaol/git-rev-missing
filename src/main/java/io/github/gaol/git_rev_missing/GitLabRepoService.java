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

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Diff;
import org.jboss.set.aphrodite.spi.RepositoryService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class GitLabRepoService extends RepoService {

    private static final String GITLAB_API_PAMA_NAME = "gitLabApi";
    private final Map<String, List<Diff>> gitlabCachedFiles = new ConcurrentHashMap<>();
    private final GitLabApi gitLabApi;

    GitLabRepoService(RepositoryService repoService) {
        super(repoService);
        gitLabApi = getInstanceField(repoService, GITLAB_API_PAMA_NAME);
    }

    @Override
    CompareResult.Result commitSame(String repoIdOrName, String sha1, String sha2, double ratioThreshold) {
        List<Diff> diffs1 = gitlabCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha1, v -> {
            try {
                return gitLabApi.getCommitsApi().getDiff(repoIdOrName, sha1);
            } catch (GitLabApiException e) {
                throw new RuntimeException("Failed to get difference of commit: " + sha1, e);
            }
        });
        List<Diff> diffs2 = gitlabCachedFiles.computeIfAbsent(repoIdOrName + "/" + sha2, v -> {
            try {
                return gitLabApi.getCommitsApi().getDiff(repoIdOrName, sha2);
            } catch (GitLabApiException e) {
                throw new RuntimeException("Failed to get difference of commit: " + sha2, e);
            }
        });
        if (diffs1.size() != diffs2.size()) {
            return CompareResult.Result.DIFFERENT;
        }
        for (int i = 0; i < diffs1.size(); i ++) {
            Diff diff1 = diffs1.get(i);
            Diff diff2 = diffs2.get(i);
            if (!diff1.getDiff().equals(diff2.getDiff())) {
                final String p1 = trimPatchLocation(diff1.getDiff());
                final String p2 = trimPatchLocation(diff2.getDiff());
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

    @Override
    void destroy() {
        try {
            super.destroy();
        } finally {
            this.gitlabCachedFiles.clear();
        }
    }
}
