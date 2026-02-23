package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "label", description = "Ignore a label")
public class IgnoreLabelCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndex index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "ID of the category.  do 'ingester show categories' to get the ID if you don't know it.")
    private String label;

    static AppLogger log = AppLogger.getLogger(IgnoreCategoryCommand.class);

    @Override
    public void run() {
        RepositoryIndex repoIndex = index.createIfNotExists(repo.trim());
        if (!repoIndex.labels.containsKey(label)) {
            index.updateDiscussionCategories(repoIndex);
        }
        if (!repoIndex.labels.containsKey(label)) {
            log.error("Label [" + label + "] not found in " + repo);
            log.info("Available labels: ");
            List<Label> labels = index.labels(repo.trim());
            for (Label label : labels) {
                log.thinking("[" + label.id() + "]: " + label.name() + " - "
                        + label.description());
            }
            return;
        }
        repoIndex.ignoredLabels.add(label);
        index.save(repoIndex);
    }

}
