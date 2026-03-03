package io.quarkiverse.github.pm;

import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.index.RepositoryConfig;
import io.quarkiverse.github.index.RepositoryConfigService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "category", description = "Ignore a discussion category")
public class IgnoreCategoryCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    RepositoryConfigService index;

    @Inject
    Github github;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "0", description = "Name of the category.  do 'ingester show categories' to get list of categories.")
    private String category;

    @Override
    public Integer call() {
        RepositoryConfig repoIndex = index.createIfNotExists(repo.trim());
        Map<String, DiscussionCategory> categories = github.repository(repo).discussionCategories();
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
