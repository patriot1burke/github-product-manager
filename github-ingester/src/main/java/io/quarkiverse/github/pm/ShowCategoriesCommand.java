package io.quarkiverse.github.pm;

import java.util.Map;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "categories", description = "Show categories for a given Github repo")
public class ShowCategoriesCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndexService index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Override
    public void run() {
        try {
            Map<String, DiscussionCategory> categories = index.discussionCategories(repo.trim());
            for (DiscussionCategory discussionCategory : categories.values()) {
                output.info("[" + discussionCategory.name() + "]: " + discussionCategory.description());
            }
        } catch (Exception e) {
            output.error("Error pulling discussions", e);
        }

    }
}
