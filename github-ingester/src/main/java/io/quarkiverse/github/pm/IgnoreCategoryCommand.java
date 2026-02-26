package io.quarkiverse.github.pm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.index.RepositoryIndex;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "category", description = "Ignore a discussion category")
public class IgnoreCategoryCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    GithubIndexService index;

    @Inject
    Github github;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "Name of the category.  do 'ingester show categories' to get list of categories.")
    private String category;

    @Override
    public Integer call() {
        RepositoryIndex repoIndex = index.createIfNotExists(repo.trim());
        IterableConnection<DiscussionCategory> discussionCategories = github.repository(repo).discussionCategories();
        Map<String, DiscussionCategory> categories = new HashMap<>();
        for (DiscussionCategory discussionCategory : discussionCategories) {
            categories.put(discussionCategory.name(), discussionCategory);
        }
        if (!categories.containsKey(category)) {
            output.error("Category [" + category + "] not found in " + repo);
            output.info("Available categories: ");
            for (DiscussionCategory discussionCategory : categories.values()) {
                output.thinking("[" + discussionCategory.name() + "]: " + discussionCategory.description());
            }
            return CommandLine.ExitCode.SOFTWARE;
        }
        repoIndex.ignoredCategories.add(category);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
