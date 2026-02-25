package io.quarkiverse.github.pm;

import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
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

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "Name of the category.  do 'ingester show categories' to get list of categories.")
    private String category;

    @Override
    public Integer call() {
        RepositoryIndex repoIndex = index.createIfNotExists(repo.trim());
        Map<String, DiscussionCategory> discussionCategories = index.discussionCategories(repo.trim());
        if (!discussionCategories.containsKey(category)) {
            output.error("Category [" + category + "] not found in " + repo);
            output.info("Available categories: ");
            for (DiscussionCategory discussionCategory : discussionCategories.values()) {
                output.thinking("[" + discussionCategory.name() + "]: " + discussionCategory.description());
            }
            return CommandLine.ExitCode.SOFTWARE;
        }
        repoIndex.ignoredCategories.add(category);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
