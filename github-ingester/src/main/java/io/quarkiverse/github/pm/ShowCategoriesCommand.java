package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "categories", description = "Show categories for a given Github repo")
public class ShowCategoriesCommand extends BaseCommand implements Runnable {
    @Inject
    Github github;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Override
    public void run() {
        try {
            IterableConnection<DiscussionCategory> categories = github.repository(repo).discussionCategories();
            for (DiscussionCategory discussionCategory : categories) {
                output.info("[" + discussionCategory.name() + "]: " + discussionCategory.description());
            }
        } catch (Exception e) {
            output.error("Error pulling discussions", e);
        }

    }
}
