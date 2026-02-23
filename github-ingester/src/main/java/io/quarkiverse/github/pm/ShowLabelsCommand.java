package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "labels", description = "Show labels for a given Github repo")
public class ShowLabelsCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndex index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus", arity = "1")
    private String repo;

    static AppLogger log = AppLogger.getLogger(ShowLabelsCommand.class);

    @Override
    public void run() {
        try {
            List<Label> labels = index.labels(repo.trim());
            for (Label label : labels) {
                log.info("[" + label.id() + "]: " + label.name() + " - "
                        + label.description());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error pulling labels", e);
        }

    }
}
