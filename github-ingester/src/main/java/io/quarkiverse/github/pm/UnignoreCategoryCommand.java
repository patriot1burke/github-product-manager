package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "category", description = "Unignore a category")
public class UnignoreCategoryCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndex index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "ID of the category.  do 'ingester show categories' to get the ID if you don't know it.")
    private String category;

    static AppLogger log = AppLogger.getLogger(UnignoreCategoryCommand.class);

    @Override
    public void run() {
        if (!index.exists(repo.trim())) {
            return;
        }
        RepositoryIndex repoIndex = index.load(repo.trim());
        if (!repoIndex.discussionCategories.containsKey(category)) {
            index.updateDiscussionCategories(repoIndex);
        }
        if (!repoIndex.discussionCategories.containsKey(category)) {
            log.error("Category [" + category + "] not found in " + repo);
            log.info("Available categories: ");
            List<DiscussionCategory> categories = index.discussionCategories(repo.trim());
            for (DiscussionCategory discussionCategory : categories) {
                log.thinking("[" + discussionCategory.id() + "]: " + discussionCategory.name() + " - "
                        + discussionCategory.description());
            }
            return;
        }
        if (!repoIndex.ignoredCategories.contains(category)) {
            return;
        }
        repoIndex.ignoredCategories.remove(category);
        index.save(repoIndex);
    }

}
