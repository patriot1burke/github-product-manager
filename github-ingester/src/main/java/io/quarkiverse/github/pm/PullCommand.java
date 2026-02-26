package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionConnectionForBasicReport;
import io.quarkiverse.github.api.Discussions.DiscussionForBasicReport;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "pull", description = "Pull issues from Github")
public class PullCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndexService discussions;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Inject
    Github github;

    static AppLogger log = AppLogger.getLogger(PullCommand.class);

    @Override
    public void run() {
        //issues.pullRepo(repo.trim());
        try {
            DiscussionConnectionForBasicReport basicReport = github.repository(repo.trim()).discussions().basicReport(100,
                    null);
            for (DiscussionForBasicReport discussion : basicReport.nodes()) {
                log.thinking("Discussion: " + discussion.category().name());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error pulling discussions", e);
        }

    }

}
