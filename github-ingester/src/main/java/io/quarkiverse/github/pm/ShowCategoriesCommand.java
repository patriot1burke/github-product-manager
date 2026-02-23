package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "categories", description = "Show categories for a given Github repo")
public class ShowCategoriesCommand extends BaseCommand implements Runnable {
    @Inject
    GithubRepoIssuesService issues;

    @Inject
    GithubIndex index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    static AppLogger log = AppLogger.getLogger(ShowCategoriesCommand.class);

    @Override
    public void run() {
        try {
            List<DiscussionCategory> categories = index.discussionCategories(repo.trim());
            for (DiscussionCategory discussionCategory : categories) {
                log.info("[" + discussionCategory.id() + "]: " + discussionCategory.name() + " - "
                        + discussionCategory.description());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error pulling discussions", e);
        }

    }
}
