package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "label", description = "Unignore a label")
public class UnignoreLabelCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndex index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "ID of the label.  do 'ingester show labels' to get the ID if you don't know it.")
    private String label;

    static AppLogger log = AppLogger.getLogger(UnignoreLabelCommand.class);

    @Override
    public void run() {
        if (!index.exists(repo.trim())) {
            return;
        }
        RepositoryIndex repoIndex = index.load(repo.trim());
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
        if (!repoIndex.ignoredLabels.contains(label)) {
            return;
        }
        repoIndex.ignoredLabels.remove(label);
        index.save(repoIndex);
    }

}
