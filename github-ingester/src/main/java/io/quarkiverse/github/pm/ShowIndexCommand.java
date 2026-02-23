package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "index", description = "Show index settings for a given Github repo")
public class ShowIndexCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndex index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    static AppLogger log = AppLogger.getLogger(ShowCategoriesCommand.class);

    @Override
    public void run() {
        if (index.exists(repo.trim())) {
            RepositoryIndex repoIndex = index.load(repo.trim());
            log.info("Ignored discussion categories: ");
            for (String id : repoIndex.ignoredCategories) {
                DiscussionCategory category = repoIndex.discussionCategories.get(id);
                log.info("   " + category.name());
            }
            log.info("Ignored labels: ");
            for (String id : repoIndex.ignoredLabels) {
                Label label = repoIndex.labels.get(id);
                log.info("   " + label.name());
            }
        }
    }
}
