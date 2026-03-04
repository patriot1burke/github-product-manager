package io.quarkiverse.github.pm;

import java.util.Map;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "labels", description = "Show labels for a given Github repo")
public class ShowLabelsCommand extends BaseCommand implements Runnable {
    @Inject
    Github github;

    @Option(names = "--repo", required = true, description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Override
    public void run() {
        try {
            Map<String, Label> labels = github.repository(repo).labels();
            for (Label label : labels.values()) {
                output.info("[" + label.name() + "]: " + label.description());
            }
        } catch (Exception e) {
            output.error("Error pulling labels", e);
        }

    }
}
