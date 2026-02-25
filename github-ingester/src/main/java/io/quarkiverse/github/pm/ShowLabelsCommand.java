package io.quarkiverse.github.pm;

import java.util.Map;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "labels", description = "Show labels for a given Github repo")
public class ShowLabelsCommand extends BaseCommand implements Runnable {
    @Inject
    GithubIndexService index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus", arity = "1")
    private String repo;

    @Override
    public void run() {
        try {
            Map<String, Label> labels = index.labels(repo.trim());
            for (Label label : labels.values()) {
                output.info("[" + label.name() + "]: " + label.description());
            }
        } catch (Exception e) {
            output.error("Error pulling labels", e);
        }

    }
}
