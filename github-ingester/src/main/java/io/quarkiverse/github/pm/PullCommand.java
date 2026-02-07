package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "pull", description = "Pull issues from Github")
public class PullCommand extends BaseCommand implements Runnable {
    @Inject
    GithubRepoIssuesService issues;

    @Inject
    GithubRepoDiscussions discussions;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    static AppLogger log = AppLogger.getLogger(PullCommand.class);

    @Override
    public void run() {
        //issues.pullRepo(repo.trim());
        try {
            discussions.pullDiscussions3(repo.trim());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error pulling discussions", e);
        }

    }

}
