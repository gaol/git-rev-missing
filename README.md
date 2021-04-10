# git-rev-missing

A simple library/tool to list missing commits when upgrading from one version to another.

This provides a capability to find potential regressions.

## How it works

Inside of the tool, it uses [Aphrodite](https://github.com/jboss-set/aphrodite) to talk with git services like github.com
or other gitlab based sites.

It lists commits from one tag/branch from some time ago(name `commitsA`), then lists commits from the other tag/branch
since the same time ago(name `commitsB`), for each commit(name `commit`) in `commitsA`, it tries to search in `commitsB` following
the below steps:

* If `SHA1` of the `commit` is found in `commitsB`, the `commit` is good.
* If `SHA1` of the `commit` is **NOT** found in `commitsB`, it tries to find the commits in `commitsB` with the same message.
* If not found, the `commit` is **missing**
* For each commit found with same message, tries to compare the diffs between the 2 commits, if the diff is the same,
the `commit` is good(like the ones using `rebase` or `cherry-pick`), otherwise, it is missing.

> NOTE It does not support `gerrit/gitweb`, it supports `github.com` and `gitlab` sites.

## How to use it

There are 2 ways to use it

### Use it as a Java library

To use this project, add the following dependency to the `dependencies` section of your build descriptor:

* Maven (in your `pom.xml`):

```xml
<dependency>
  <groupId>io.github.gaol</groupId>
  <artifactId>git-rev-missing</artifactId>
  <version>${maven.version}</version>
</dependency>
```

* Gradle (in your `build.gradle` file):

```groovy
compile 'io.github.gaol:git-rev-missing:${maven.version}'
```

Then take the following example on how to call the API.

```java
public class TestApp {
    public static void main(String[] args) throws Exception {
      GitRevMissing gitRevMissing = GitRevMissing.create(new URL("https://github.com"), username, access_token);
      MissingCommit missCommit = gitRevMissing.missingCommits("ihomeland", "prtest", "revA", "revB");
      if (missCommit.isClean()) {
          logger.info("Great, no missing commits found");
      } else {
          logger.warn("\n  " + missCommit.getCommits().size() + " commits were missing in " + revB + "\n");
          logger.warn(missCommit.toString());
      }
      gitRevMissing.release();
    }
}
```

### Use `git_rev_missing.sh` script

There is a script `git_rev_missing.sh` can be used to run directly like the following example: 

```shell script
git clone https://github.com/gaol/git-rev-missing
cd git-rev-missing
./git_rev_missing.sh https://github.com/ihomeland/prtest/compare/revA...revB
```

The above example tries to find commits in revA, but missing in revB of https://github.com/ihomeland/prtest.

You will see the following similar output:

```shell script
Apr 10, 2021 8:23:59 PM git_rev_missing.impl missingCommits
INFO: 3 commits are found in revision: revA since: 2020-04-10T06:23:57.623Z
Apr 10, 2021 8:23:59 PM git_rev_missing.impl missingCommits
INFO: 3 commits are found in revision: revB since: 2020-04-10T06:23:57.623Z
Apr 10, 2021 8:23:59 PM git_rev_missing.main call
WARNING: 
  1 commits were missing in revB

Apr 10, 2021 8:23:59 PM git_rev_missing.main call
WARNING: {
  "commits" : [ {
    "commit" : {
      "sha" : "6a7d8dd7fae154653d04b5c0ca6184b3bd40c107",
      "message" : "Update 2nd in a separate file"
    },
    "commitLink" : "https://github.com/ihomeland/prtest/commit/6a7d8dd7fae154653d04b5c0ca6184b3bd40c107"
  } ],
  "clean" : false
}
```

### Config File

Please refer to [config.json.example](./config.json.example) for the configuration format:
```json
{
  "repositoryConfigs": [
    {
      "url": "https://github.com/",
      "username": "my-user-name",
      "password": "my-access-token",
      "type": "GITHUB"
    },
    {
      "url": "https://gitlab.xxx.yyy.com/",
      "username": "my-user-name",
      "password": "my-access-token",
      "type": "GITLAB"
    }
  ]
}
```

It has the same format as what Aphrodite has.
